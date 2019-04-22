package lz.dubbo.dispatcher;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.remoting.Dispatcher;
import com.alibaba.dubbo.rpc.Protocol;

import lz.dubbo.threadPool.SeprExecutorService;



//不使用spi 而是通过spring的方式做dubbo扩展
@Component
public class DubboDispatcherConfig implements InitializingBean,DisposableBean {
	private final Log logger=LogFactory.getLog(this.getClass());
	
	private SeprDispatcher dispatcher;
	
	@Autowired
	private SeprExecutorService executorService;
	
	
	@Override
	public void destroy() throws Exception {
		executorService.close();
		ExtensionLoader.getExtensionLoader(Protocol.class).getExtension("dubbo").destroy();
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		
		ExtensionLoader.getExtensionLoader(Dispatcher.class).replaceExtension("all", 
				this.getClass().getClassLoader().loadClass("lz.dubbo.dispatcher.SeprDispatcher"));		
		dispatcher=(SeprDispatcher) ExtensionLoader.getExtensionLoader(Dispatcher.class).getExtension("all");
		dispatcher.setExecutorService(executorService);
		
	}

}
