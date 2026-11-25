package cl.induztek.ms_equipos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@org.springframework.context.annotation.Import(cl.induztek.comun.CoordinacionReferencias.class)
@SpringBootApplication
@EnableFeignClients(basePackages = {"cl.induztek.ms_equipos", "cl.induztek.comun"})
public class MsEquiposApplication {

	public static void main(String[] args) {
		SpringApplication.run(MsEquiposApplication.class, args);
	}

}
