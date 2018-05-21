package com.example.orderservice;

import lombok.*;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.util.Date;

@Entity(name = "orders")
@Getter
@Setter
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {

	@Id
	@GeneratedValue
	private Long id;
	private Date datetime;
	private String state;

	public Order(Date datetime, OrderStates state) {
		this.datetime = datetime;
		this.setOrderState(state);
	}

	public OrderStates getOrderStates() {
		return OrderStates.valueOf(this.state);
	}

	public void setOrderState(OrderStates s) {
		this.state = s.name();
	}
}
