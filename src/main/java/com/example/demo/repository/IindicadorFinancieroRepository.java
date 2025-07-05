package com.example.demo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.models.IndicadorFinanciero;

@Repository
public interface IindicadorFinancieroRepository extends JpaRepository<IndicadorFinanciero, Integer> {
 //Busca un indicador financiero según su relación con un costeo
	Optional<IndicadorFinanciero> findByCosteoIdCosteo(Integer idCosteo);
}
