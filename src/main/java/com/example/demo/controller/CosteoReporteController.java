package com.example.demo.controller;

import com.example.demo.DTO.ReporteViewDTO;
import com.example.demo.DTO.TrabajoDTO;
import com.example.demo.models.ReporteTrabajo;
import com.example.demo.models.view.VistaReporteCosteo;
import com.example.demo.repository.ReporteTrabajoRepository;
import com.example.demo.repositoryView.VistaReporteCosteoRepository;
import com.example.demo.service.PdfReportService;

import jakarta.servlet.http.HttpServletResponse;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/costeo")
public class CosteoReporteController {

    @Autowired
    private VistaReporteCosteoRepository reporteRepo;

    @Autowired
    private PdfReportService pdfService;
    
    @Autowired
    private ReporteTrabajoRepository reporteRepository;

    @GetMapping("/listar")
    public String listar(Model model) {
        // Traigo todos para extraer zonas únicas con su id
        List<VistaReporteCosteo> todos = reporteRepo.findAll();
        Map<Long, String> zonasUnicas = todos.stream()
            .collect(Collectors.toMap(
                VistaReporteCosteo::getIdZona,
                VistaReporteCosteo::getZonaTrabajo,
                (zona1, zona2) -> zona1 // en caso de duplicado, queda uno
            ));

        model.addAttribute("zonas", zonasUnicas);
        return "costeo/form_listar";
    }

    @PostMapping("/pdf")
    public String generarPdf(
            @RequestParam("idZona") Long idZona,
            @RequestParam("fechaInicio") String fechaInicio,
            @RequestParam("fechaFin") String fechaFin,
        //para mostrar mensajes
            RedirectAttributes redirect,
        //permite escribir el PDF como descarga
            HttpServletResponse response) throws IOException {

    	String interfaz= null;
        // 1) Consulta y trae datos filtrados y debug
        List<VistaReporteCosteo> reportes =
            pdfService.fetchReportesPorZonaYFechas(idZona, fechaInicio, fechaFin);

       //verificamos si la lista reportes esta vacia o es null para mostrar mensaje en vista
        if (reportes.isEmpty() || reportes == null) {
            System.out.println("[DEBUG] No hay datos filtrados, devolviendo todos.");
            
            redirect.addFlashAttribute("error",
                    "No se encontraron para Zona ID=" + idZona +
                    ". Por favor elija otra fecha");
            
            interfaz = "redirect:/costeo/listar";
        }else {
        
        BigDecimal cFijo = BigDecimal.ZERO;
        BigDecimal cVariable = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal ganancia = BigDecimal.ZERO;
        BigDecimal margen = BigDecimal.ZERO;
        BigDecimal rentabilidad = BigDecimal.ZERO;
        
        List<TrabajoDTO> listaTrabajoDTO = new ArrayList<>();
       
        //Se recorre cada VistaReporteCosteo
        for(VistaReporteCosteo vrc : reportes) {
            ////Se suman los valores para obtener totales.
        	cFijo = cFijo.add(vrc.getCostoFijo());
        	cVariable = cVariable.add(vrc.getCostoVariable());
        	total = total.add(vrc.getCostoTotal());
        	ganancia = ganancia.add(vrc.getGananciaObtenida());
       System.out.println("NOMBRE TRABAJO : " + vrc.getNombreTrabajo());
       System.out.println("FECHA : " + vrc.getFecha());
           TrabajoDTO tb = new TrabajoDTO();
        	tb.setNombre(vrc.getNombreTrabajo());
        	tb.setFecha(vrc.getFecha());
            //se crea una lista de trabajos (TrabajoDTO) para mostrar en el PDF
        	listaTrabajoDTO.add(tb);
        	 System.out.println("NOMBRE TRABAJO OBJETO: " + tb.getNombre());
             System.out.println("FECHA OBJETO: " + tb.getFecha());
        }
            //calcula indicadores:margen y rentabilidad
        BigDecimal valorInterno = ganancia.add(total);
        margen = (ganancia.divide(valorInterno,4,RoundingMode.HALF_UP)).multiply(BigDecimal.valueOf(100));
        rentabilidad = (ganancia.divide(total,4,RoundingMode.HALF_UP)).multiply(BigDecimal.valueOf(100));
        
        
        for(int i=0;i<listaTrabajoDTO.size();i++) {
        	 System.out.println("NOMBRE TRABAJO LISTA: " + listaTrabajoDTO.get(i).getNombre());
             System.out.println("FECHA LISTA: " + listaTrabajoDTO.get(i).getFecha());
        }
            //Este objeto contiene el resumen del reporte para pasarlo al generador de PDF
        ReporteViewDTO reporteView = new ReporteViewDTO();
        reporteView.setZonaTrabajo(reportes.get(0).getZonaTrabajo());
        reporteView.setCostoFijo(cFijo);
        reporteView.setCostoVariable(cVariable);
        reporteView.setCostoTotal(total);
        reporteView.setGananciaObtenida(ganancia);
        reporteView.setMargenGanancia(margen);
        reporteView.setRentabilidad(rentabilidad);
        System.out.println("FECHA HOY: " + reporteView.getFecha());
        // 3) Generar PDF
        byte[] pdf = pdfService.generatePdfBytes(reporteView,listaTrabajoDTO);
        String filename = "Reporte_Costeo_Zona" + idZona + ".pdf";
        
         if(pdf!=null) {
        	 ModelMapper modelMapper = new ModelMapper();
        	 ReporteTrabajo rTrabajo = modelMapper.map(reporteView, ReporteTrabajo.class);
        	 reporteRepository.save(rTrabajo);
         }
         
        // 4) Enviar al cliente
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                           "attachment; filename=" + filename);
        response.setContentType(MediaType.APPLICATION_PDF_VALUE);
        response.getOutputStream().write(pdf);
       
    }
        return interfaz; // ya enviamos PDF
    }
}
