package cn.org.craftsmen.mcv.servlet;

import cn.org.craftsmen.mcv.annotation.Controller;
import cn.org.craftsmen.mcv.annotation.Qualifier;
import cn.org.craftsmen.mcv.annotation.RequestMapping;
import cn.org.craftsmen.mcv.annotation.Service;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

public class DispatcherServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    //读取配置
    private Properties properties = new Properties();
    //类的全路径名集合
    private List<String> classNames = new ArrayList<>();
    //ioc
    private Map<String, Object> ioc = new HashMap<>();
    //保存url和controller的关系
    private Map<String, Method> handlerMapping = new HashMap<>();

    @Override
    public void init(ServletConfig config) throws ServletException {
        //加载配置文件
        doLoadConfig(config.getInitParameter("contextConfigLocation"));
        //初始化所有相关的类
        doScanner(properties.getProperty("scanPackage"));
        //拿到扫描到的类，通过反射机制，实例化并且放到ioc容器中
        doInstance();
        //初始化HandlerMapping(将url和method对应上)、url-实例 存放到ioc
        initHandlerMapping();
        //实现注入
        doIoc();
    }

    private void doLoadConfig(String location) {
        //将web.xml中的contextConfigLocation对应的value值得文件加载到流
        if (location.startsWith("classpath:")) {
            location = location.replace("classpath:", "");
        } else if (location.contains("/")) {
            int lastSplitIndex = location.lastIndexOf('/');
            location = location.substring(lastSplitIndex + 1, location.length());
        }

        InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(location);
        try {
            properties.load(resourceAsStream);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != resourceAsStream) {
                try {
                    resourceAsStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void doScanner(String packageName) {
        String theurl = packageName.replace(".", "/");
        URL url = this.getClass().getClassLoader().getResource("/" + packageName.replace(".", "/"));
        File dir = new File(url.getFile());
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                //递归读取package
                doScanner(packageName + "." + file.getName());
            } else {
                String className = packageName + "." + file.getName().replace(".class", "");
                classNames.add(className);
                System.out.println("Spring容器扫描到的类有：" + packageName + "." + file.getName());
            }
        }
    }

    private void doInstance() {
        if (classNames.isEmpty()) {
            return;
        }
        for (String className : classNames) {
            try {
                //通过反射实例化
                Class<?> clazz = Class.forName(className);
                if (clazz.isAnnotationPresent(Controller.class)) {
                    Controller controller = clazz.getAnnotation(Controller.class);
                    String key = controller.value();
                    if (!"".equals(key) && key != null) {
                        ioc.put(key, clazz.newInstance());
                    } else {
                        ioc.put(toLowerFirstWord(clazz.getSimpleName()), clazz.newInstance());
                    }
                } else if (clazz.isAnnotationPresent(Service.class)) {
                    Service service = clazz.getAnnotation(Service.class);
                    String key = service.value();
                    if (!"".equals(key) && key != null) {
                        ioc.put(key, clazz.newInstance());
                    } else {
                        ioc.put(toLowerFirstWord(clazz.getSimpleName()), clazz.newInstance());
                    }
                } else {
                    continue;
                }
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }
        }
    }

    private void initHandlerMapping() {
        if (ioc.isEmpty()) {
            return;
        }
        try {
            //存放controller的 url-method
            Map<String, Object> url_method = new HashMap<>();
            for (Map.Entry<String, Object> entry : ioc.entrySet()) {
                Class<? extends Object> clazz = entry.getValue().getClass();
                if (!clazz.isAnnotationPresent(Controller.class)) {
                    continue;
                }
                //url拼接，controller url 拼接 方法 url
                String baseUrl = "";
                if (clazz.isAnnotationPresent(RequestMapping.class)) {
                    RequestMapping annotation = clazz.getAnnotation(RequestMapping.class);
                    baseUrl = annotation.value();
                }
                Method[] methods = clazz.getMethods();
                for (Method method : methods) {
                    if (!method.isAnnotationPresent(RequestMapping.class)) {
                        continue;
                    }
                    if (method.isAnnotationPresent(RequestMapping.class)) {
                        RequestMapping annotation = method.getAnnotation(RequestMapping.class);
                        String url = annotation.value();

                        url = (baseUrl + "/" + url).replaceAll("/+", "/");
                        handlerMapping.put(url, method);
                        System.out.println(url + "," + method);
                        url_method.put(url, clazz.newInstance());
                    }
                }
                ioc.putAll(url_method);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void doIoc() {
        if (ioc.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            Field fields[] = entry.getValue().getClass().getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                if (field.isAnnotationPresent(Qualifier.class)) {
                    String value = field.getAnnotation(Qualifier.class).value();
                    field.setAccessible(true);
                    String key;
                    if (!"".equals(value) && value != null) {
                        key = value;
                    } else {
                        key = field.getName();
                    }
                    try {
                        field.set(entry.getValue(), ioc.get(key));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            doDispatch(req, resp);
        } catch (Exception e) {
            resp.getWriter().write("500!! Server Exception");
        }
    }


    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        if (handlerMapping.isEmpty()) {
            return;
        }

        String url = req.getRequestURI();
        String contextPath = req.getContextPath();

        url = url.replace(contextPath, "").replaceAll("/+", "/");

        if (!this.handlerMapping.containsKey(url)) {
            resp.getWriter().write("404 NOT FOUND!");
            return;
        }
        Method method = this.handlerMapping.get(url);

        //获取方法的参数列表
        Class<?>[] parameterTypes = method.getParameterTypes();

        //获取请求的参数
        Map<String, String[]> parameterMap = req.getParameterMap();

        //保存参数值
        Object[] paramValues = new Object[parameterTypes.length];

        //方法的参数列表
        for (int i = 0; i < parameterTypes.length; i++) {
            String requestParam = parameterTypes[i].getSimpleName();
            if (requestParam.equals("HttpServletRequest")) {
                paramValues[i] = req;
                continue;
            }
            if (requestParam.equals("HttpServletResponse")) {
                paramValues[i] = resp;
                continue;
            }
            if (requestParam.equals("String")) {
                for (Map.Entry<String, String[]> param : parameterMap.entrySet()) {
                    String value = Arrays.toString(param.getValue()).replaceAll("\\[|\\]", "");
                    paramValues[i] = value;
                }
            }
        }
        //利用反射机制来调用
        try {
            method.invoke(this.ioc.get(url), paramValues);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private String toLowerFirstWord(String className) {
        int size = className.length();
        String a = className.substring(0, 1).toLowerCase();
        String b = className.substring(1, size);
        return a + b;
    }
}
