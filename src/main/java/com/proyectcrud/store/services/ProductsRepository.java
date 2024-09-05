package com.proyectcrud.store.services;

import com.proyectcrud.store.models.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductsRepository extends JpaRepository <Product, Integer> {// Cambiado de Integer a Long
}
