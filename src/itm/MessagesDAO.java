package itm; 

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

/**
 * MessagesDAO es una clase que proporciona un m�todo est�tico interfaz para obtener todos los mensajes asociados a un usuario
 * 
 * Los mensaje se organizan a partir del mensaje padre, que contiene una lista de mensajes respuesta
 */

public class MessagesDAO {
	
	//longitud m�xima en n�mero de caracteres del resumen de mensaje
	private static final int SUMMARY_LENGTH = 100; 
	
	static Connection currentCon = null;
	static ResultSet rs = null;
	
	/**
	 * M�otodo para recuperar todos los mensajes asociados a un usuario en forma de lista de conversaciones.
	 */
	public static List<ConversationBean> getMessages(UserBean currentUser) {
		
		List<ConversationBean> conversations = new ArrayList<ConversationBean>();
		//List<MessageBean> result = new ArrayList<MessageBean>();
		
		PreparedStatement stmt = null;    
															//Seleccionamos los mensajes recibidos por el usuario
		String searchQuery = "SELECT * FROM messages,users WHERE (messages.idreceiver = ? and users.idusers = messages.idsender )" +
															//Seleccionamos los mensajes enviados por el usuario
													         "or (messages.idsender = ? and users.idusers = messages.idreceiver )" +
															"ORDER BY timestamp";
															
		    
		try {
			//connect to DB 
			currentCon = ConnectionManager.getConnection();
			stmt = currentCon.prepareStatement(searchQuery);
			
			stmt = currentCon.prepareStatement(searchQuery);
			stmt.setLong(1, currentUser.getId() );
			stmt.setLong(2, currentUser.getId() );
			
			rs = stmt.executeQuery();	        

			//Mientras haya m�s mensajes en el resultado...
			while (rs.next()) {
				
				MessageBean b = new MessageBean();
				b.setId( rs.getLong("idmessages") );
				b.setTimestamp( rs.getDate("timestamp") );
				b.setContent( rs.getString("content") );
				
				// Summary son las primeras palabras del mensaje
				// Se genera autom�ticamente a partir del propio mensaje con la constante SUMMARY_LENGTH
				String summary = b.getContent();
				if ( SUMMARY_LENGTH < summary.length() ){
					int position = SUMMARY_LENGTH;
					while ( summary.charAt(position) != ' ' ) {
						position--;
					}
					summary = summary.substring(0,position) + " ...";
				}
				b.setSummary(summary);
				b.setAnswerId( rs.getInt("answer") );
				
				
				b.setIdReceiver( rs.getLong("idreceiver") );
				b.setIdSender( rs.getLong("idsender") );
				
				// Recuperamos el otro usuario que participa en la conversaci�n
				UserBean other_user = new UserBean();
				other_user.setId(rs.getLong("idsender"));
				other_user.setUsername( rs.getString("username") );
				other_user.setFirstName (rs.getString("firstname") );
				other_user.setLastName( rs.getString("lastname") );
				other_user.setRegion( rs.getString("region") );
				
				// Comprobamos si este mensaje es original o una respuesta a una conversaci�n existente
				// En caso de ser una nueva conversaci�n, la creamos
				if ( b.getAnswerId() == -1 )
					conversations.add( new ConversationBean (currentUser.getId(), other_user ,b) );
					
				else {
					// Si el mensaje es una respuesta, tendr� una conversaci�n ya existente en la lista conversations
					for (ConversationBean r : conversations){
						// La id de los objetos ConversationBean se corresponde con la id del primer mensaje que contienen
						if ( b.getAnswerId() == r.getId() ){
							//El mensaje respuesta se a�ade como atributo a la lista de respuestas del mensaje padre
							//De esta forma queda inclu�do en la lista de mensajes
							//Suponemos que los mensajes obtenidos de la BBDD se recuperan en orden cronol�gico
							r.addMessage(b);
							
						}
					}
				}

			}
		
		}catch (Exception ex) {
		     System.err.println(ex);
		} 
	    
		  //some exception handling
		finally {
		     if (rs != null)	{
		        try {
		           rs.close();
		        } catch (Exception e) { System.err.println(e); }
		           rs = null;
		        }
		
		     if (stmt != null) {
		        try {
		           stmt.close();
		        } catch (Exception e) { System.err.println(e); }
		           stmt = null;
		        }
		
		     if (currentCon != null) {
		        try {
		           currentCon.close();
		        } catch (Exception e) { System.err.println(e); }
		
		        currentCon = null;
		     }
		}
				
		return conversations;
					
	}

	/**
	 * M�otodo para a�adir mensajes nuevos a la base de datos.
	 * Si conversation_id == -1, entonces es un nuevo mensaje. Si tiene un valor, entonces es una respuesta
	 */
	public static boolean sendMessage ( UserBean currentSessionUser, long conversation_id, long other_user_id, String content){
		
		PreparedStatement stmt = null;    
		boolean exito = true;
		String searchQuery = "INSERT INTO messages ( idsender, idreceiver, answer, content )" +
				                           "VALUES ( ?,?,?,? )";
		
		try {
			
			currentCon = ConnectionManager.getConnection();
			stmt = currentCon.prepareStatement(searchQuery);
			
			stmt = currentCon.prepareStatement(searchQuery);
			stmt.setLong(1, currentSessionUser.getId() );
			stmt.setLong(2, other_user_id );
			stmt.setLong(3, conversation_id );
			stmt.setString(4, content );
			
			stmt.executeUpdate();
			
		}catch (Exception ex) {
		     System.err.println(ex);
		     exito = false;
		} 
	    
		  //some exception handling
		finally {
		     if (rs != null)	{
		        try {
		           rs.close();
		        } catch (Exception e) { System.err.println(e); }
		           rs = null;
		        }
		
		     if (stmt != null) {
		        try {
		           stmt.close();
		        } catch (Exception e) { System.err.println(e); }
		           stmt = null;
		        }
		
		     if (currentCon != null) {
		        try {
		           currentCon.close();
		        } catch (Exception e) { System.err.println(e); }
		
		        currentCon = null;
		     }
		}
		
		return exito;
	}
}