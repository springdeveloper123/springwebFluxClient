package com.example.demo.client.rest;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import com.example.demo.client.model.Employee;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/client")
public class SpringWebFluxClient {
	
	private WebClient client = WebClient.create("http://localhost:8080");
	
	@GetMapping
	public Flux<Employee> getEmployees() {
		return client.get().uri("/employee")
				.accept(MediaType.APPLICATION_JSON)
				.retrieve()
				.bodyToFlux(Employee.class)
				.log("retrieve emps:: ");
	}
	
	@GetMapping("/exchange")
	public Flux<Employee> getEmployeesByExchange() {
		return client.get().uri("/employee")
				.accept(MediaType.APPLICATION_JSON)
				.exchange().flatMapMany(
						r -> r.bodyToFlux(Employee.class)
						).log("exchange emps::");
	}
	
	@GetMapping("/{id}")
	public Mono<Employee> getEmployeeById(@PathVariable("id") String id) {
		return client.get().uri("/employee/{id}", id).accept(MediaType.APPLICATION_JSON)
				.retrieve().bodyToMono(Employee.class).log("empById::");
	}

	@PostMapping
	public Mono<Employee> createEmployee(@RequestBody Employee emp) {
		return client.post().uri("/employee").accept(MediaType.APPLICATION_JSON)
				.body(BodyInserters.fromObject(emp)).retrieve()
				.bodyToMono(Employee.class)
				.log("insert using body");
	}
	
	@PostMapping("/sync")
	public Mono<Employee> createEmployee_sync(@RequestBody Employee emp) {
		return client.post().uri("/employee").accept(MediaType.APPLICATION_JSON)
				.syncBody(emp)
				.retrieve()
				.bodyToMono(Employee.class)
				.log("insert using body");
	}
	
	@PutMapping("/{id}")
	public Mono<Employee> updateEmployee(@RequestBody Employee emp, @PathVariable("id") String id) {
		return client.put().uri("/employee/{id}", id).accept(MediaType.APPLICATION_JSON)
				.syncBody(emp)
				.retrieve()
				.bodyToMono(Employee.class)
				.log("update emp::");
	}
	
	@DeleteMapping("/{id}")
	public Mono<Void> deleteEmployee(@PathVariable("id") String id) {
		return client.delete().uri("/employee/{id}", id).accept(MediaType.APPLICATION_JSON)
				.retrieve().bodyToMono(Void.class)
				.log("delete emp::");
	}
	
	@GetMapping("/error")
	public Mono<Employee> getEmpByError() {
		return client.get().uri("/emp/error").accept(MediaType.APPLICATION_JSON)
				.retrieve()
				.onStatus(HttpStatus::is4xxClientError, clientResponse ->{
					System.out.print("4xx error occurred");
				return clientResponse.bodyToMono(Throwable.class);
				}
				)
				.onStatus(HttpStatus::is5xxServerError, clientResponse ->{
					Mono<String> errorMsg = clientResponse.bodyToMono(String.class);
					return errorMsg.flatMap(msg ->{
						System.out.println("error msg ::"+ msg);
						throw new RuntimeException(msg);
					});
				}).bodyToMono(Employee.class);
	}
	
	@GetMapping("/exchange/error")
	public Mono<Employee> getEmployee_error() {
		return client.get().uri("/emp/error").accept(MediaType.APPLICATION_JSON)
				.exchange().flatMap(
						clientResponse -> {
							if(clientResponse.statusCode().is5xxServerError()) {
								Mono<String> errorMsg = clientResponse.bodyToMono(String.class);
								return errorMsg.flatMap(msg ->{
									System.out.println("error msg ::"+ msg);
									throw new RuntimeException(msg);
								});
							}
							return clientResponse.bodyToMono(Employee.class);
						}
						);
	}
	
}
