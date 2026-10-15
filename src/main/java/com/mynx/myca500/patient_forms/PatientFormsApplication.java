package com.mynx.myca500.patient_forms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = { "com.mynx.myca500.patient_forms", "com.mynx.myca500.commons" })
public class PatientFormsApplication {

	public static void main(String[] args) {
		SpringApplication.run(PatientFormsApplication.class, args);
	}

}
