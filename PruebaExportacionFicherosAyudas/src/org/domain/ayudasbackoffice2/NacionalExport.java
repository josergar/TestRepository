package org.domain.ayudasbackoffice2;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;

import javax.activation.DataHandler;
import javax.xml.namespace.QName;
import javax.xml.ws.soap.MTOMFeature;

import org.apache.commons.io.IOUtils;
//import org.jboss.utilidades.NoExisteException;
//
//import com.sun.xml.ws.developer.JAXWSProperties;

import es.dxd.oepm.repositorio.webservicesfg.DatosDocumentoFG;
import es.dxd.oepm.repositorio.webservicesfg.InputBeanFG;
import es.dxd.oepm.repositorio.webservicesfg.MetadatosGenericosFG;
import es.dxd.oepm.repositorio.webservicesfg.OutputBeanFG;
import es.dxd.oepm.repositorio.webservicesfg.RepositorioDocumentalFGWS;
import es.dxd.oepm.repositorio.webservicesfg.RepositorioDocumentalFGWSService;


public class NacionalExport {
	
	// prueba
	
	/*
	 * OJO, PORQUE SI PARA UNA SOLICITUD NO SE ENCUENTRA NINGUN FICHERO QUE CUMPLA LAS CONDICIONES 
	 * ENTONCES NO SE CREARA LA CARPETA DEL EXPEDIENTE EN CONCRETO Y TAL VEZ DEBA EXISTIR LA CARPETA DEL EXPEDIENTE
	 * AUNQUE LUEGO EST� VAC�A.
	 * 
	 * TAMBI�N EXISTE UN PROBLEMA CON ALGUNOS CASOS MUY CONCRETOS DE NOMBRES DE FICHEROS QUE CONTIENEN CARACTERES QUE NO 
	 * SE PERMITEN COMO NOMBRES DE FICHEROS EN WINDOWS. Por ejemplo, '?' o '�', etc.... EN ESOS CASOS TAL VEZ HAYA QUE SUSTITUIR
	 * DICHOS CARACTERES POR GUIONES.
	 */
	public static final String consultaNacionales = 
			"select COD_SOLICITUD, NOMBRE, ID_CONTENIDO, ID_DOCUMENTO, FECHA_ANEXO " +
			"from ( " +
			"select sol.ID_SOLICITUD, exp.COD_SOLICITUD, fich.NOMBRE, fich.ID_CONTENIDO, fich.ID_DOCUMENTO, fich.FECHA_ANEXO " +
			"from AY2_BACK_SOLICITUDES sol, AY2_BACK_EXPEDIENTE exp, AY2_BACK_FICHEROS fich " +
			"where sol.COD_CONVOCATORIA = '2017' " +
			"  and sol.ID_SOLICITUD = exp.ID_SOLICITUD " +
			"  and exp.ID_PROGRAMA = 2 " +
			"  and exp.TOTAL_A_PAGAR > 0 " +
			"  and (select count(*) " +
			"       from AY2_BACK_EXPEDIENTE exp2 " +
			"	   where exp2.ID_PROGRAMA = 3 " +
			"	     and substr(exp.COD_SOLICITUD, 0, 16) = substr(exp2.COD_SOLICITUD, 0, 16) " +
			"		 and exp2.TOTAL_A_PAGAR > 0 " +
			"	   ) =  0 " +
			"  and exp.ID_SOLICITUD = fich.ID_SOLICITUD " +
			"  and fich.FECHA_ANEXO >= TO_DATE('11/09/2017 00:00', 'dd/MM/yyyy HH24:MI') " +
//			"  and (fich.COD_TIPO_FICHERO = 'ALEGA' or fich.COD_TIPO_FICHERO = 'RECUR') " +
			"  and (fich.NOMBRE like 'Resolucion definitiva%.pdf') " +
			") " +
			"order by COD_SOLICITUD ASC, FECHA_ANEXO DESC";
	
	public final static String rutaBaseNacional = "D:\\tmp\\2017\\cd\\1.Nacional\\";

	public static void main(String[] args) {
		try {
			Class.forName("oracle.jdbc.driver.OracleDriver");
		} catch (ClassNotFoundException e) {
			System.out.println("Where is your Oracle JDBC Driver?");
			e.printStackTrace();
			return;
		}

		System.out.println("Oracle JDBC Driver Registered!");

		Connection connection = null;
		try {
			// DESARROLLO
			connection = DriverManager.getConnection("jdbc:oracle:thin:@otdesa-scan.oepm.local:1523:oltpdesa2", "GESTAYUDAS", "Oenu857HNByhf");
		} catch (SQLException e) {
			System.out.println("Conexi�n a BD fallida");
			e.printStackTrace();
			return;
		}

		if (connection != null) {
			System.out.println("Conexi�n a BD realizada...");
			
			Statement stmt;
			try {
				RepositorioDocumentalFGWS port = abrirConexionUCM(); // Conexi�n a UCM

				stmt = connection.createStatement();
				ResultSet rs = stmt.executeQuery(consultaNacionales);
				int contador = 0;
				while (rs.next()) {
					//COD_SOLICITUD, NOMBRE, ID_CONTENIDO, ID_DOCUMENTO, FECHA_ANEXO
					String codSolicitud = rs.getString("COD_SOLICITUD");
					String nombre = rs.getString("NOMBRE");
					String idContenido = rs.getString("ID_CONTENIDO");
					String idDocumento = rs.getString("ID_DOCUMENTO");
					Date fechaAnexo = rs.getDate("FECHA_ANEXO");
					
					//System.out.println(codSolicitud + " - " + nombre + " - " + idContenido + " - " + fechaAnexo);
					File ruta = new File(rutaBaseNacional + codSolicitud);
					if (ruta.mkdirs()) {
						System.out.println("Creando carpeta: " + ruta.getAbsolutePath());
					} else {
						// carpeta ya creada anteriormente, no se ha vuelto a crear.
					}
					
					escribeFicheroUCM(port, idContenido, idDocumento, rutaBaseNacional + codSolicitud + "\\" + nombre);
					
				}
				
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			System.out.println("Conexi�n a BD fallida2");
		}
		System.out.println("Fin del proceso");

	}
	
	public static RepositorioDocumentalFGWS abrirConexionUCM() {
        // DESARROLLO
    	String userUcm = "App_Ayudas2_GD_DES";        
      String passUcm = "App_Ayudas2_GD_DES01";
      String urlUcm = "http://pruebaucm.oepm.local/aInterfazWSUCM11gFGMTOM/RepositorioDocumentalFGWSService?WSDL";
        
        URL url;
		try {
			url = new URL(urlUcm);
	        RepositorioDocumentalFGWSService service = new RepositorioDocumentalFGWSService(
	                url, new QName(
	                        "http://webservicesfg.repositorio.oepm.dxd.es/",
	                        "RepositorioDocumentalFGWSService"));
	        RepositorioDocumentalFGWS port = service
	                .getRepositorioDocumentalFGWSPort(new MTOMFeature());
	        return port;
		} catch (MalformedURLException e) {
			System.out.println(e.getMessage());
			return null;
		}
	}
	
    public static void escribeFicheroUCM(RepositorioDocumentalFGWS port, String idContenido, String idDocumento, String rutaArchivo)
    {
        InputBeanFG ib = new InputBeanFG();
        DatosDocumentoFG datosDocumento = new DatosDocumentoFG();
        ib.setDatosDoc(datosDocumento);

        MetadatosGenericosFG metaGen = new MetadatosGenericosFG();

        // DESARROLLO
    	String userUcm = "App_Ayudas2_GD_DES";        
       String passUcm = "App_Ayudas2_GD_DES01";
        
        metaGen.setUsuario(userUcm);
        metaGen.setPassword(passUcm);
        metaGen.setIdContenido(idContenido);
        metaGen.setIdDocumento(idDocumento);

        ib.getDatosDoc().setMetaGen(metaGen);

        OutputBeanFG respuesta = port.recuperarDocumentoEspecificoFG(ib);
        //log.info("Respuesta: " + respuesta.getResultado());

        if (respuesta.getResultado().equals("1")) {
            System.out.println("NO hemos obtenido fichero de UCM: " + respuesta.getListaDatosDoc()[0].getMetaEsp()[0].getNombre()
                + ": "
                + respuesta.getListaDatosDoc()[0].getMetaEsp()[0].getValor());
//                throw new NoExisteException(
//                        "NO hemos obtenido fichero de UCM:"
//                                + respuesta.getListaDatosDoc()[0].getMetaEsp()[0]
//                                        .getValor());

        } else {
            System.out.println("SI hemos obtenido fichero de UCM:" + rutaArchivo); 
            if (respuesta.getListaDatosDoc() != null) {
                DataHandler dataHandlerResp = respuesta.getListaDatosDoc()[0]
                        .getDocumento().getContenido();
                InputStream is;
				try {
					is = dataHandlerResp.getInputStream();
	                OutputStream os = new FileOutputStream(new File(rutaArchivo));
	                // This will copy the file from the two streams
                   IOUtils.copy(is, os);

                   // This will close two streams catching exception
                   IOUtils.closeQuietly(os);
                   IOUtils.closeQuietly(is);
    			} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            } else
                System.out.println("El documento no existe.");
        }
    }
	

}
