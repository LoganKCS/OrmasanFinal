package com.example.demo.repositoryView;

import com.example.demo.models.view.VistaReporteCosteo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface VistaReporteCosteoRepository extends JpaRepository<VistaReporteCosteo, Long> {
//consulta JPQL personalizada
    @Query("""
        SELECT v 
        FROM VistaReporteCosteo v
        WHERE v.idZona = :idZona
          AND v.fecha BETWEEN :desde AND :hasta
    """)
    List<VistaReporteCosteo> findByIdZonaAndFechaBetween(
        @Param("idZona") Long idZona,
        @Param("desde") LocalDate desde,
        @Param("hasta") LocalDate hasta
    );

}

