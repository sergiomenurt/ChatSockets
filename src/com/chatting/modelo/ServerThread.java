package com.chatting.modelo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import com.chatting.ejecutable.Servidor;
import com.chatting.vista.VistaServidor;

public class ServerThread extends Thread {

	private Socket cliente;
	private BufferedReader entrada;
	private PrintWriter salida;	
	private VistaServidor vista;
	
	private String nombre;
	
	public ServerThread(VistaServidor vista, Socket cliente) throws IOException {
		this.vista = vista;
		this.cliente = cliente;
		nombre = "";
		entrada = new BufferedReader(new InputStreamReader(cliente.getInputStream()));
		salida = new PrintWriter(cliente.getOutputStream(), true);
	}
	
	public void run() {
		String cadena;
		try {
			do {
				cadena = recibirTCP();
				messageHandler(cadena.trim());
			}while(!cadena.trim().equals(Constantes.CODIGO_SALIDA));
			
			entrada.close();
			salida.close();
			cliente.close();
		}catch(IOException e) { Servidor.imprimirEnTodos("<SERVER> "+nombre+" desconectado dolorosamente."); }
		Servidor.clientesConectados--;
		vista.setClientesConectados(Servidor.clientesConectados);
	}
	
	public void imprimirEnCliente(String msg) {
		enviarTCP(msg);
	}
	
	private void messageHandler(String mensaje) {
		switch(mensaje) {
			case Constantes.CODIGO_NICK:
				
				String nombreAnterior = nombre;
				nombre = recibirTCP();
				if(nombreAnterior.equals("")) 
					Servidor.imprimirEnTodos("<SERVER> "+ nombre+ " se ha unido al chat.");
				else {
					Servidor.sacarCliente(nombreAnterior);
					Servidor.imprimirEnTodos("<SERVER> "+ nombreAnterior + " ha cambiado su nombre por "+ nombre +".");
				}
				Servidor.meterCliente(this);
				
			case Constantes.CODIGO_SALIDA:
				
				Servidor.imprimirEnTodos("<SERVER> "+ nombre+ " ha abandonado el chat.");
				Servidor.sacarCliente(nombre);
				
			break;
			case Constantes.CODIGO_CONECTADOS:
				
				enviarTCP(String.valueOf(Servidor.clientesConectados));
				
			break;
			case Constantes.CODIGO_MAX_CLIENTES:
				
				enviarTCP(String.valueOf(Constantes.MAX_CONEXIONES));
				
			break;
			case Constantes.CODIGO_LISTAR:
				
				enviarTCP(Servidor.obtenerListadoClientes());
				
			break;
			default:
				
				Servidor.imprimirEnTodos(nombre+": "+ mensaje);
				
			break;
		}
	}
	
	/**
	 * Espera hasta recibir una cadena y envía confirmación.
	 * @return
	 */
	private String recibirTCP() {
		String cadenaRecibida = "";
		do {
			try {
				cadenaRecibida = entrada.readLine();
			} catch (IOException e) { cadenaRecibida = ""; }
		} while(!cadenaRecibida.trim().contains(Constantes.CODIGO_FIN_CADENA));
			salida.println(Constantes.CODIGO_RECIBIDO_CADENA);
		return cadenaRecibida.subSequence(0, cadenaRecibida.length()-(Constantes.CODIGO_FIN_CADENA).length()).toString();
	}
	
	/**
	 * Envía un dato hasta que reciba confimación de llegada.
	 * @param cadena
	 */
	private void enviarTCP(String cadena) {
		String comprobante;
		do {
			salida.println(cadena + Constantes.CODIGO_FIN_CADENA);
			try {
				comprobante = entrada.readLine().trim();
			} catch (IOException e) { comprobante = "";	}
		}while(!comprobante.equals(Constantes.CODIGO_RECIBIDO_CADENA));
	}
	
	public String getNombre() {
		return nombre;
	}
}