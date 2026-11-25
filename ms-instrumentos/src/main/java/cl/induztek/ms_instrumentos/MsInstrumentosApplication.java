package cl.induztek.ms_instrumentos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@org.springframework.context.annotation.Import(cl.induztek.comun.CoordinacionReferencias.class)
@SpringBootApplication
@EnableFeignClients(basePackages = {"cl.induztek.ms_instrumentos", "cl.induztek.comun"})
public class MsInstrumentosApplication {

	public static void main(String[] args) {
		SpringApplication.run(MsInstrumentosApplication.class, args);
	}

}
