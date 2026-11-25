package cl.induztek.ms_protocolos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@org.springframework.context.annotation.Import(cl.induztek.comun.CoordinacionReferencias.class)
@SpringBootApplication
@EnableFeignClients
public class MsProtocolosApplication {

	public static void main(String[] args) {
		SpringApplication.run(MsProtocolosApplication.class, args);
	}

}
