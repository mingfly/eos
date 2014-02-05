/**
 * @(#)AbstractServiceContext
 * 版权声明 厦门畅享信息技术有限公司, 版权所有 违者必究
 *
 *<br> Copyright:  Copyright (c) 2014
 *<br> Company:厦门畅享信息技术有限公司
 *<br> @author ulyn
 *<br> 14-1-31 下午5:37
 *<br> @version 1.0
 *————————————————————————————————
 *修改记录
 *    修改者：
 *    修改时间：
 *    修改原因：
 *————————————————————————————————
 */
package com.sunsharing.eos.common.config;

import com.sunsharing.eos.common.annotation.EosService;
import com.sunsharing.eos.common.utils.ClassFilter;
import com.sunsharing.eos.common.utils.ClassHelper;
import com.sunsharing.eos.common.utils.ClassUtils;
import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;

import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <pre></pre>
 * <br>----------------------------------------------------------------------
 * <br> <b>功能描述:</b>
 * <br>
 * <br> 注意事项:
 * <br>
 * <br>
 * <br>----------------------------------------------------------------------
 * <br>
 */
public abstract class AbstractServiceContext {
    Logger logger = Logger.getLogger(AbstractServiceContext.class);

    protected ApplicationContext ctx;
    protected String packagePath;

    //存储服务对象,key为服务id
    protected static Map<String, Object> services = new HashMap<String, Object>();
    protected static List<ServiceConfig> serviceConfigList = new ArrayList<ServiceConfig>();

    public AbstractServiceContext(ApplicationContext ctx, String packagePath) {
        this.ctx = ctx;
        this.packagePath = packagePath;

        String xmlConfigFileName = "EosServiceConfig.xml";
        //key为接口name
        Map<String, ServiceConfig> xmlServiceConfigMap = loadXmlServiceConfig(xmlConfigFileName);

        ClassFilter filter = new ClassFilter() {
            @Override
            public boolean accept(Class clazz) {
                if (Modifier.isInterface(clazz.getModifiers())) {
                    Annotation ann = clazz.getAnnotation(EosService.class);
                    if (ann != null) {
                        return true;
                    }
                }
                return false;
            }
        };
        List<Class> classes = ClassUtils.scanPackage(packagePath, filter);

        for (final Class c : classes) {
            ServiceConfig config = new ServiceConfig();
            EosService ann = (EosService) c.getAnnotation(EosService.class);

            String id = getBeanId(c, ann.id());
            config.setId(id);
            config.setAppId(ann.appId());
            config.setProxy(ann.proxy());
            config.setSerialization(ann.serialization());
            config.setTimeout(ann.timeout());
            config.setTransporter(ann.transporter());
            config.setVersion(ann.version());
            config.setImpl(ann.impl());

            if (xmlServiceConfigMap.containsKey(c.getName())) {
                //有xml配置的使用xml配置
                ServiceConfig xmlConfig = xmlServiceConfigMap.get(c.getName());
                config.setMock(xmlConfig.getMock());
                if (!"".equals(xmlConfig.getImpl())) {
                    config.setImpl(xmlConfig.getImpl());
                }
            }

            Object bean = createBean(c, config);
            serviceConfigList.add(config);
            services.put(config.getId(), bean);
        }
    }

    /**
     * 从xml文件加载服务配置,目前只读接口的mock参数
     *
     * @param fileName
     * @return
     */
    private Map<String, ServiceConfig> loadXmlServiceConfig(String fileName) {
        Map<String, ServiceConfig> configMap = new HashMap<String, ServiceConfig>();
        InputStream is = ClassHelper.getClassLoader(ServiceConfig.class).getResourceAsStream(fileName);
        if (is == null) {
            logger.info("没有找到eos服务的xml配置...");
        } else {
            //TODO 加载xml生成configMap
        }
        return configMap;
    }

    /**
     * 根据服务id取得服务bean，接口服务id不能有重复，否则可能得不到想要的结果
     *
     * @param id
     * @param <T>
     * @return
     */
    public static <T> T getBean(String id) {
        Object o = services.get(id);
        if (o == null) {
            return null;
        }
        return (T) o;
    }

    private String getBeanId(Class interfaces, String id) {
        if (id.equals("")) {
            id = interfaces.getSimpleName();
            id = Character.toLowerCase(id.charAt(0)) + id.substring(1);
        }
        return id;
    }

    public static List<ServiceConfig> getServiceConfigList() {
        return serviceConfigList;
    }

    /**
     * 创建bean对象
     *
     * @param interfaces
     * @param config
     * @return
     */
    protected abstract Object createBean(Class interfaces, ServiceConfig config);
}

