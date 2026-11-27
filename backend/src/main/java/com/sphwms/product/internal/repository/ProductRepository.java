package com.sphwms.product.internal.repository;

import com.sphwms.product.internal.entity.Product;
import com.sphwms.product.internal.enums.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByActiveTrueOrderByProductIdAsc();

    Optional<Product> findByNameIgnoreCase(String name);

    @Query("SELECT p FROM Product p WHERE LOWER(p.name) = LOWER(:name) AND p.productId <> :id")
    Optional<Product> findByNameIgnoreCaseAndIdNot(@Param("name") String name, @Param("id") Long id);

    @Query("SELECT p.category, COUNT(p) FROM Product p WHERE p.active = true GROUP BY p.category")
    List<Object[]> countActiveByCategory();

    long countByActiveTrue();
}
