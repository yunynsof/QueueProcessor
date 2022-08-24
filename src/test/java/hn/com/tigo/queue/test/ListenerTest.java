package hn.com.tigo.queue.test;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.Test;

import hn.com.tigo.queue.dto.DetailQueueDTO;
import hn.com.tigo.queue.listener.ProcessQueueMasterThread;
import hn.com.tigo.queue.utils.ReadFilesConfig;

public class ListenerTest {

	@Test
	public void test() {
		DetailQueueDTO queue = new DetailQueueDTO();
		queue.setQueueIPAddress("192.168.0.231");
		queue.setQueueUser("USRCPE");
		queue.setQueuePassword("SRCP2015");
		queue.setQueuelibName("V4STCD");
		queue.setQueueName("MGTEL004I");
		queue.setQueueSplit("|");
		
		ProcessQueueMasterThread thread = new ProcessQueueMasterThread(queue);
		thread.params = new HashMap<String, String>();
		thread.params.put("SUBSCRIBER_SPLIT", "=");
		thread.params.put("URL_NOTIFY_EVENT", "http://192.168.159.46:7004/NotifyEvent/NotifyQueue/dist");
		ReadFilesConfig readConfig = new ReadFilesConfig();
		long startTime = 0;
		String tramaComplete = "SEGUROS|SEGUROS|MSISDN=94517500|CUENTA_CLIENTE=0801198716839CCHN|CUENTA_FACTURACION=8000066364|ID_PEDIDO_VENTA=1521895736507599298|NUMERO_FACTURA=3330|FECHAEMISION=20200213|IMEI=869175040414278|TOTAL_FACTURADO=100.00|TIPO_FACTURA=FC2|USUARIO=HCORDERO|MOTIVO=PAGO|TEST_QA|                 123432 ";
		String tramaComplete2 = "FACTURACION|FACTURACION|MSISDN=94517500|CUENTA_CLIENTE=0801198716839CCHN|CUENTA_FACTURACION=8000066364|ID_PEDIDO_VENTA=1521895736507599298|NUMERO_FACTURA=3330|FECHAEMISION=20200213|IMEI=869175040414278|TOTAL_FACTURADO=100.00|TIPO_FACTURA=FC2|USUARIO=HCORDERO|MOTIVO=PAGO|TEST_QA|                 123432 ";
		String tramaComplete3 = "PAGOS|PAGOS|MSISDN=94517500|CUENTA_CLIENTE=0801198716839CCHN|CUENTA_FACTURACION=8000066364|ID_PEDIDO_VENTA=1521895736507599298|NUMERO_FACTURA=3330|FECHAEMISION=20200213|IMEI=869175040414278|TOTAL_FACTURADO=100.00|TIPO_FACTURA=FC2|USUARIO=HCORDERO|MOTIVO=PAGO|TEST_QA|                 123432 ";
		String tramaComplete4 = "CONSULTA|CONSULTA|MSISDN=94517500|CUENTA_CLIENTE=0801198716839CCHN|CUENTA_FACTURACION=8000066364|ID_PEDIDO_VENTA=1521895736507599298|NUMERO_FACTURA=3330|FECHAEMISION=20200213|IMEI=869175040414278|TOTAL_FACTURADO=100.00|TIPO_FACTURA=FC2|USUARIO=HCORDERO|MOTIVO=PAGO|TEST_QA|                 123432 ";
		String tramaComplete5 = "PAGOS_DEACTIVATE|PAGOS_DEACTIVATE|MSISDN=94517500|CUENTA_CLIENTE=0801198716839CCHN|CUENTA_FACTURACION=8000066364|ID_PEDIDO_VENTA=1521895736507599298|NUMERO_FACTURA=3330|FECHAEMISION=20200213|IMEI=869175040414278|TOTAL_FACTURADO=100.00|TIPO_FACTURA=FC2|USUARIO=HCORDERO|MOTIVO=PAGO|TEST_QA|                 123432 ";
		
		String tramaComplete6 = "SEGUROS|SEGUROS|MSISDN=|CUENTA_CLIENTE=0801198716839CCHN|CUENTA_FACTURACION=8000066364|ID_PEDIDO_VENTA=1521895736507599298|NUMERO_FACTURA=3330|FECHAEMISION=20200213|IMEI=869175040414278|TOTAL_FACTURADO=100.00|TIPO_FACTURA=FC2|USUARIO=HCORDERO|MOTIVO=PAGO|TEST_QA|                 123432 ";
		String tramaComplete7 = "FACTURACION|FACTURACION|MSISDN=|CUENTA_CLIENTE=0801198716839CCHN|CUENTA_FACTURACION=8000066364|ID_PEDIDO_VENTA=1521895736507599298|NUMERO_FACTURA=3330|FECHAEMISION=20200213|IMEI=869175040414278|TOTAL_FACTURADO=100.00|TIPO_FACTURA=FC2|USUARIO=HCORDERO|MOTIVO=PAGO|TEST_QA|                 123432 ";
		String tramaComplete8 = "PAGOS|PAGOS|MSISDN=|CUENTA_CLIENTE=0801198716839CCHN|CUENTA_FACTURACION=8000066364|ID_PEDIDO_VENTA=1521895736507599298|NUMERO_FACTURA=3330|FECHAEMISION=20200213|IMEI=869175040414278|TOTAL_FACTURADO=100.00|TIPO_FACTURA=FC2|USUARIO=HCORDERO|MOTIVO=PAGO|TEST_QA|                 123432 ";
		String tramaComplete9 = "CONSULTA|CONSULTA|MSISDN=|CUENTA_CLIENTE=0801198716839CCHN|CUENTA_FACTURACION=8000066364|ID_PEDIDO_VENTA=1521895736507599298|NUMERO_FACTURA=3330|FECHAEMISION=20200213|IMEI=869175040414278|TOTAL_FACTURADO=100.00|TIPO_FACTURA=FC2|USUARIO=HCORDERO|MOTIVO=PAGO|TEST_QA|                 123432 ";
		String tramaComplete10 = "PAGOS_DEACTIVATE|PAGOS_DEACTIVATE|MSISDN=|CUENTA_CLIENTE=0801198716839CCHN|CUENTA_FACTURACION=8000066364|ID_PEDIDO_VENTA=1521895736507599298|NUMERO_FACTURA=3330|FECHAEMISION=20200213|IMEI=869175040414278|TOTAL_FACTURADO=100.00|TIPO_FACTURA=FC2|USUARIO=HCORDERO|MOTIVO=PAGO|TEST_QA|                 123432 ";
		
		List<String> list = new ArrayList<String>();
		list.add(tramaComplete);
		list.add(tramaComplete2);
		list.add(tramaComplete3);
		list.add(tramaComplete4);
		list.add(tramaComplete5);
		list.add(tramaComplete6);
		list.add(tramaComplete7);
		list.add(tramaComplete8);
		list.add(tramaComplete9);
		list.add(tramaComplete10);
		list.add("");
		
		for(int i=0; i<list.size(); i++) {
			
			try {
				thread.processTrama(readConfig, startTime, list.get(i));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}

}
