/**
 * ProcessQueueMasterThread.java
 * ProcessQueueMasterThread
 * Copyright (c) Tigo Honduras.
 */
package hn.com.tigo.queue.listener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;

import hn.com.tigo.josm.persistence.core.ServiceSessionEJB;
import hn.com.tigo.josm.persistence.core.ServiceSessionEJBLocal;
import hn.com.tigo.josm.persistence.exception.PersistenceException;
import hn.com.tigo.queue.as400.AbstractDriverQueue;
import hn.com.tigo.queue.dto.AttributeValuePair;
import hn.com.tigo.queue.dto.ConfigEventDTO;
import hn.com.tigo.queue.dto.DetailEventDTO;
import hn.com.tigo.queue.dto.DetailQueueDTO;
import hn.com.tigo.queue.dto.NotifyMessageDTO;
import hn.com.tigo.queue.listener.entity.OcepManager;
import hn.com.tigo.queue.utils.QueueConstantListener;
import hn.com.tigo.queue.utils.ReadFilesConfig;
import hn.com.tigo.queue.utils.States;

/**
 * This class contains the main logic for reading events to be processed and send them to JMS queues..
 *
 * @author Leonardo Vijil
 * @version 1.0.0
 * @since 11/02/2020 11:10:03 AM 2020
 */
public class ProcessQueueMasterThread extends Thread {

	/**
	 * This attribute will store an instance of log4j for ProcessQueueMasterThread
	 * class.
	 */
	private static final Logger LOGGER = LogManager.getLogger(ProcessQueueMasterThread.class);

	/**
	 * The executor service, allows the execution of a thread pool.
	 */
	private ThreadPoolExecutor executorService;

	/** The state. */
	private States state;

	/** The config. */
	private final DetailQueueDTO config;

	/** The params. */
	private Map<String, String> params;

	/**
	 * Instantiates a new process queue master thread.
	 *
	 * @param config the config
	 */
	public ProcessQueueMasterThread(final DetailQueueDTO config) {

		this.config = config;
		try {
			initialize();
		} catch (Exception e) {
			state = States.SHUTTINGDOWN;
			LOGGER.error(QueueConstantListener.UNABLE_INITIALIZE + e.getMessage(), e);
		}
	}

	/**
	 * Method that allow to initialize the executor thread.
	 */
	public void initialize() {
		BlockingQueue<Runnable> workingQueue = new ArrayBlockingQueue<Runnable>(1);
		LOGGER.info("workingQueue correctly");
		executorService = new ThreadPoolExecutor(1, 1, 1, TimeUnit.MILLISECONDS, workingQueue);
		// Starting master thread
		state = States.STARTED;
		LOGGER.info("Iinitialize Finalized.");
	}

	/**
	 * Shutdown.
	 */
	public void shutdown() {
		state = States.SHUTTINGDOWN;
		executorService.shutdownNow();
	}


	/**
	 * Run.
	 */
	@Override
	public void run() {
		getConnection();
		AbstractDriverQueue connqueue = null;
		while (state == States.STARTED) {
			
			ReadFilesConfig readConfig = null;
			long startTime = 0;
			
			try {
				readConfig = new ReadFilesConfig();
				if (connqueue == null) {
					connqueue = new AbstractDriverQueue(config);
				}
				String tramaComplete =  connqueue.readQueue();

				startTime = processTrama(readConfig, startTime, tramaComplete);
			} catch (Exception error) {
				LOGGER.error(QueueConstantListener.NAME_LISTENER + this.getClass().getName()
						+ QueueConstantListener.MESSAGE_ERROR_PROCESS, error);
				NewRelicImpl.addNewRelicError(error);
			} finally {
				long endTime = System.nanoTime(); // Se guarda el tiempo final del proceso.
				long duration = (endTime - startTime); // Se calcula el tiempo que tomo procesar los datos.
				long timeDuration = duration / 1000000;
				NewRelicImpl.addNewRelicMetric("QueueProcessorAgent", timeDuration); // Se manda la informacion de
																							// la duracion del proceso a
																							// NewRelic como metrica.
				startTime = 0;
				if (connqueue != null && state != States.STARTED) {
						connqueue.disconnectService();
						connqueue = null;
				}
			}
		}
		executorService.shutdown();
	}

	/**
	 * Process trama.
	 *
	 * @param readConfig the read config
	 * @param startTime the start time
	 * @param tramaComplete the trama complete
	 * @return the long
	 */
	public long processTrama(ReadFilesConfig readConfig, long startTime, String tramaComplete) throws IOException  {
		if (tramaComplete != null) {
			startTime = System.nanoTime();
			String[] trama = tramaComplete.split("\\|");

			final ConfigEventDTO configEvent = readConfig.readConfigEvent();
			DetailEventDTO detailEvent = readConfig.getDetailEvent(configEvent, trama[0]);

			if (detailEvent != null) {
				LOGGER.info("Trama: " + tramaComplete + " Evento: " + detailEvent.getName() + " ProductId: "
						+ detailEvent.getDefaultProduct());
				String request = obtainRequest(trama, detailEvent);

				if (request != null) {
					methodPost(request);
				}

			} else {
				LOGGER.info("Trama no aceptada, por no tener evento valido: " + tramaComplete);
			}
		}
		return startTime;
	}

	/**
	 * Obtain request.
	 *
	 * @param trama the trama
	 * @param detailEvent the detail event
	 * @return the string
	 */
	private String obtainRequest(String[] trama, DetailEventDTO detailEvent) {
		String request = null;

		NotifyMessageDTO notifyMessageDTO = new NotifyMessageDTO();
		String evenType = trama[0];
		String uuid = UUID.randomUUID().toString();
		Calendar cycleCalendar = Calendar.getInstance();
		final SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");
		String split = params.get("SUBSCRIBER_SPLIT");

		switch (evenType) {
		case "SEGUROS":
			String[] subscriber = trama[2].split(split);
			String subscId = "";
			if (subscriber.length > 1) {
				subscId = subscriber[1];
			} else {
				subscriber = trama[3].split(split);
				subscId = subscriber[1];
			}
			NotifyMessageDTO object = generatedRequest(trama, detailEvent, notifyMessageDTO, evenType, uuid, subscId,
					df.format(cycleCalendar.getTime()));
			Gson gson = new Gson();
			return gson.toJson(object);

		case "FACTURACION":
			String[] subscriber2 = trama[2].split(split);
			String subscId2 = "";
			if (subscriber2.length > 1) {
				subscId2 = subscriber2[1];
			} else {
				subscriber2 = trama[3].split(split);
				subscId2 = subscriber2[1];
			}
			NotifyMessageDTO object2 = generatedRequest(trama, detailEvent, notifyMessageDTO, evenType, uuid, subscId2,
					df.format(cycleCalendar.getTime()));
			Gson gson2 = new Gson();
			return gson2.toJson(object2);

		case "PAGOS":
			String[] subscriber3 = trama[2].split(split);
			String subscId3 = "";
			if (subscriber3.length > 1) {
				subscId3 = subscriber3[1];
			} else {
				subscriber3 = trama[3].split(split);
				subscId3 = subscriber3[1];
			}
			NotifyMessageDTO object3 = generatedRequest(trama, detailEvent, notifyMessageDTO, evenType, uuid, subscId3,
					df.format(cycleCalendar.getTime()));
			Gson gson3 = new Gson();
			return gson3.toJson(object3);

		case "CONSULTA":
			String[] subscriber4 = trama[2].split(split);
			String subscId4 = "";
			if (subscriber4.length > 1) {
				subscId4 = subscriber4[1];
			} else {
				subscriber4 = trama[3].split(split);
				subscId4 = subscriber4[1];
			}
			NotifyMessageDTO object4 = generatedRequest(trama, detailEvent, notifyMessageDTO, evenType, uuid, subscId4,
					df.format(cycleCalendar.getTime()));
			Gson gson4 = new Gson();
			return gson4.toJson(object4);

		case "PAGOS_DEACTIVATE":
			String[] subscriber5 = trama[2].split(split);
			String subscId5 = "";
			if (subscriber5.length > 1) {
				subscId5 = subscriber5[1];
			} else {
				subscriber5 = trama[3].split(split);
				subscId5 = subscriber5[1];
			}
			NotifyMessageDTO object5 = generatedRequest(trama, detailEvent, notifyMessageDTO, evenType, uuid, subscId5,
					df.format(cycleCalendar.getTime()));
			Gson gson5 = new Gson();
			return gson5.toJson(object5);
		default:
			return request;
		}
	}

	/**
	 * Generated request.
	 *
	 * @param trama the trama
	 * @param detailEvent the detail event
	 * @param notifyMessageDTO the notify message DTO
	 * @param evenType the even type
	 * @param uuid the uuid
	 * @param subscriber the subscriber
	 * @param fecha the fecha
	 * @return the notify message DTO
	 */
	private NotifyMessageDTO generatedRequest(String[] trama, DetailEventDTO detailEvent,
			NotifyMessageDTO notifyMessageDTO, String evenType, String uuid, String subscriber, String fecha) {
		notifyMessageDTO.setEventType(evenType);
		notifyMessageDTO.setChannelId(String.valueOf(detailEvent.getChannelId()));
		notifyMessageDTO.setOrderType(detailEvent.getDefaultOrder());
		notifyMessageDTO.setProductId(Integer.valueOf(detailEvent.getDefaultProduct()));
		notifyMessageDTO.setSubscriberId(subscriber);
		notifyMessageDTO.setDate(fecha);
		notifyMessageDTO.setTransactionId(uuid);
		notifyMessageDTO.setAdditionalsParams(getAdditionalParams(trama));
		return notifyMessageDTO;
	}

	/**
	 * Gets the additional params.
	 *
	 * @param trama the trama
	 * @return the additional params
	 */
	private List<AttributeValuePair> getAdditionalParams(String[] trama) {

		List<AttributeValuePair> list = new ArrayList<AttributeValuePair>();

		for (int i = 0; i < trama.length; i++) {
			AttributeValuePair attributeValuePair = new AttributeValuePair();
			if (i == 0) {

				attributeValuePair.setAttribute("EVENT");
				attributeValuePair.setValue(trama[0]);
				list.add(attributeValuePair);
			} else if (i == 1) {

				attributeValuePair.setAttribute("SUBEVENT");
				attributeValuePair.setValue(trama[1]);
				list.add(attributeValuePair);
			} else {

				String tramaF = trama[i];
				if ((i+1) == trama.length) {
					tramaF = tramaF.replaceAll(" ", "");
					tramaF = tramaF.replaceAll("\u0000", "");
					tramaF = tramaF.replaceAll("\u000f", "");
				}

				String[] attribute = tramaF.split(params.get("SUBSCRIBER_SPLIT"));
				if (!attribute[0].equals("")) {
					attributeValuePair.setAttribute(attribute[0]);
					attributeValuePair.setValue(attribute.length > 1 ? attribute[1] : "");
					list.add(attributeValuePair);
				}
			}
		}
		return list;
	}

	/**
	 * Method post.
	 *
	 * @param request the request
	 * @return the string
	 */
	private String methodPost(String request) {
		StringBuilder content = null;
		try {

			String urlFinal = params.get("URL_NOTIFY_EVENT");
			LOGGER.info(urlFinal);

			URL url = new URL(urlFinal);
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setDoOutput(true);
			con.setRequestMethod("POST");
			con.setConnectTimeout(5000);
			con.setReadTimeout(5000);
			con.setRequestProperty("Content-Type", "application/json");

			byte[] outputInBytes = request.getBytes("UTF-8");
			OutputStream os = con.getOutputStream();
			os.write(outputInBytes);
			os.flush();

			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String inputLine;
			content = new StringBuilder();
			while ((inputLine = in.readLine()) != null) {
				content.append(inputLine);
			}
			in.close();
			con.disconnect();
			LOGGER.info(content.toString());
		} catch (Exception e) {
			LOGGER.error("ERROR: en api Notifyevent: " + e.getMessage());
			return "";
		}
		return content.toString();
	}
	
	
	/**
	 * Gets the connection.
	 *
	 * @return the connection
	 */
	@SuppressWarnings("unchecked")
	private void getConnection() {
		OcepManager manager = null;
		try {
			ServiceSessionEJBLocal<OcepManager> serviceSession = ServiceSessionEJB.getInstance();
			manager = serviceSession.getSessionDataSource(OcepManager.class,
					QueueConstantListener.DATASOURCE_CPE);
			params = manager.listAllParam();
		} catch (PersistenceException e) {
			LOGGER.error(QueueConstantListener.ERROR_CONNECTION + e.getMessage(), e);
		} finally {
			if (manager != null) {
				try {
					manager.close();
				} catch (PersistenceException e) {
					LOGGER.error(QueueConstantListener.COULD_NOT_CLOSE);
				}
			}
		}
	}



	/**
	 * Sets the params.
	 *
	 * @param params the params
	 */
	public void setParams(Map<String, String> params) {
		this.params = params;
	}

}
