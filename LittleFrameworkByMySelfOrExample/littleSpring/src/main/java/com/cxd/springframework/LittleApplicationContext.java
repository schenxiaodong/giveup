package com.cxd.springframework;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.beans.Introspector;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

public class LittleApplicationContext {

    private Class configClass;

    private Map<String, BeanDefinition> beanDefinitionMap = new HashMap<>();

    private Map<String, Object> singletonObjects = new HashMap<>(); // 单例池

    public LittleApplicationContext(Class configClass) {
        this.configClass = configClass;

        // 先进行包扫描，然后将需要的定义信息都保存为BeanDefinition
        scan(configClass);

        // 判断beanDefinition中的Bean是否是单例且非懒加载的Bean，如果是，就先创建出来缓存在单例池中

        for (String beanName : beanDefinitionMap.keySet()) {
            BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);
            if ("singleton".equalsIgnoreCase(beanDefinition.getScope()) && !beanDefinition.isLazy()) {
                Object o = createBean(beanName, beanDefinition);
                singletonObjects.put(beanName, o);
            }
        }

    }

    /**
     * 创建Bean
     *
     * @param beanName
     * @param beanDefinition
     * @return
     */
    private Object createBean(String beanName, BeanDefinition beanDefinition) {
        Class clazz = beanDefinition.getType();
        try {
            // 先实例化Bean
            Object o = clazz.newInstance();

            // 属性注入
            Field[] declaredFields = clazz.getDeclaredFields();
            for (Field declaredField : declaredFields) {
                if (declaredField.isAnnotationPresent(Autowired.class)) {
                    Object object = getBean(declaredField.getName());
                    declaredField.setAccessible(true); // 打开暴力反射
                    declaredField.set(o, object);
                }
            }

            // BeanNameAware回调
            if (o instanceof BeanNameAware) {
                ((BeanNameAware) o).setBeanName(beanName);
            }

            // ApplicationContextAware 回调
            if (o instanceof ApplicationContextAware) {
                ((ApplicationContextAware) o).setApplicationContext(this);
            }

            // 事务控制，这里采取的是对类上面的所有方法都增强
            if (clazz.isAnnotationPresent(Transactional.class)) {
                // 采用cglib生产Enhancer方式生产代理对象，Enhancer增强器
                Enhancer enhancer = new Enhancer(); // 创建Enhancer对象，用于生成代理类
                enhancer.setSuperclass(clazz); // 设置代理类的父类，即被代理类的类对象
                Object target = o; // 将目标对象保存到变量中
                enhancer.setCallback(new MethodInterceptor() { // 设置方法拦截器
                    @Override
                    public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
                        System.out.println("开启事务"); // 日志输出
                        Object result = method.invoke(target, objects); // 调用目标对象的方法
                        System.out.println("提交事务"); // 日志输出
                        return result;  // 返回方法执行结果
                    }
                });

                o = enhancer.create(); // 使用Enhancer生成代理类的实例，并将其赋给变量o
            }

            return o;
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private void scan(Class configClass) {
        // 先查看配置类是否有注解包扫描路径，如果没有注解Spring容器就会是个空的
        boolean annotationPresent = configClass.isAnnotationPresent(ComponentScan.class);
        if (annotationPresent) {
            ComponentScan annotation = (ComponentScan) configClass.getAnnotation(ComponentScan.class);
            String packageScanPath = annotation.value();
            // 将class路径. 替换成文件路径 /
            packageScanPath = packageScanPath.replaceAll("\\.", "/");

            // 定位到包扫描路径，例如： file:/D:/Codes/IDEAWorkspace/LittleFrameworkByMySelfOrExample/littleSpring/target/classes/com/cxd/user/service
            URL resource = this.getClass().getClassLoader().getResource(packageScanPath);
            // 两层嵌套扫描包扫描路径下所有的*.class文件，demo工程，就不写嵌套查找文件了
            // 加入list规则，是文件，且以后缀.class结尾
            List<File> classFile = new ArrayList<>();
            if (resource != null) {
                File file = new File(resource.getFile());
                if (file.isDirectory()) {
                    for (File file1 : file.listFiles()) {
                        if (file1.isDirectory()) {
                            for (File file2 : file1.listFiles()) {
                                if (file2.isFile() && file2.getName().endsWith(".class")) {
                                    classFile.add(file2);
                                }
                            }
                        } else if (file1.getName().endsWith(".class")) {
                            classFile.add(file1);
                        }
                    }
                }  else if (file.getName().endsWith(".class")) {
                    classFile.add(file);
                }
            }

            // 找到class文件后对class文件进行筛选。
            // 需要类上面注解了@Component的才是我们需要找的类，并将他们的信息赋给BeanDefinition
            for (File file: classFile) {
                String fileName = file.getPath();
                String className = fileName.substring(fileName.indexOf("com"), fileName.indexOf(".class")).replaceAll("\\\\", ".");

                // 通过类加载形式加载该类，然后得到上面的信息
                try {
                    Class<?> clazz = this.getClass().getClassLoader().loadClass(className);
                    // 如果类上面有@Component注解才会被Spring进行管理
                    if (clazz.isAnnotationPresent(Component.class)) {
                        Component component = clazz.getAnnotation(Component.class);
                        BeanDefinition beanDefinition = new BeanDefinition();
                        beanDefinition.setType(clazz);  // 设置Type

                        // 查看是否懒加载（类是否被@Lazy注解）
                        if (clazz.isAnnotationPresent(Lazy.class)) {
                            Lazy lazy = clazz.getAnnotation(Lazy.class);
                            beanDefinition.setLazy(lazy.value());
                        } else {
                            beanDefinition.setLazy(false);
                        }

                        // 查看Bean的作用域
                        if (clazz.isAnnotationPresent(Scope.class)) {
                            Scope scope = clazz.getAnnotation(Scope.class);
                            if ("prototype".equalsIgnoreCase(scope.value())) {
                                beanDefinition.setScope("prototype");
                            } else {
                                beanDefinition.setScope("singleton");
                            }
                        } else {
                            beanDefinition.setScope("singleton");
                        }
                        // 构建Bean的name，如果有注解BeanName就使用注解的，否则使用bean的首字母小写的名字
                        String simpleClassName = component.value();
                        if (StringUtil.isEmpty(simpleClassName)) {
                            String simpleName = clazz.getSimpleName();
                            char[] charArray = simpleName.toCharArray();
                            charArray[0] = Character.toLowerCase(charArray[0]);
                            simpleClassName = new String(charArray);
                            // spring官方采用的首字母小写的方法
                            // simpleClassName = Introspector.decapitalize(clazz.getSimpleName());
                        }
                        beanDefinitionMap.put(simpleClassName, beanDefinition);
                    }
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public Object getBean(String beanName) {
        BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);
        if (beanDefinition == null) {
            throw new RuntimeException("找不到 " + beanName + " 的beanDefinition");
        }

        if ("singleton".equalsIgnoreCase(beanDefinition.getScope())) {
            Object singletonObject = singletonObjects.get(beanName);
            if (null == singletonObject) {  // 可能部分bean是懒加载的
                singletonObject = createBean(beanName, beanDefinition);
                singletonObjects.put(beanName, singletonObject);
            }
            return singletonObject;
        } else if ("prototype".equalsIgnoreCase(beanDefinition.getScope())){ // 原型bean就现场构造
            Object prototypeObject = createBean(beanName, beanDefinition);
            return prototypeObject;
        }

        return null;
    }
}
