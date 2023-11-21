package principal;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class Main {
	
	static Conexiondb conexiondb = new Conexiondb();
	static Connection db=conexiondb.Cn();
	
	public static void main(String[] args) throws Throwable {
		// TODO Auto-generated method stub
		
		
		int result=1;
		String sql;
		int mdc_dias_vencidos;
		int total_dias_vencidos=0;
		Date mdc_fecha_vencimiento;
		int milisecondsByDay;
		int smn_mov_documento_cob_cab_id;
		double scl_saldo_vencido_ml=0;
		double scl_saldo_vencido_ma=0;
		double mdc_saldo_doc_ml;
		double mdc_saldo_doc_ma;
		int smn_cliente_rf;
		double scl_porcentaje_deuda_vencida=0;
		double scl_porcentaje_deuda_vencida_ma=0;
		int smn_entidad_rf=0;
		int smn_sucursal_rf=0;
		double scl_saldo_inicial_ml=0.00;
		double scl_saldo_inicial_ma=0.00;
		double scl_saldo_final_ml=0.00;
		double scl_saldo_final_ma=0.00;
		double scl_debitos_ml=0.00;
		double scl_debitos_ma=0.00;
		double scl_creditos_ml=0.00;
		double scl_creditos_ma=0.00;
		double scl_monto_doc_registrados_ml=0.00;
		double scl_monto_doc_registrados_ma=0.00;
		int smn_saldo_cliente_id=0;
		Date scl_fecha=null;
		
		int hora;
		int minutos;
		int segundos;
		String hora_actual;
		String scl_estatus="SO";
		
		double scl_promedio_dias_vencido=0.00;
		int cantidad_docs=0;
		
		Calendar calendario = Calendar.getInstance();
		hora =calendario.get(Calendar.HOUR_OF_DAY);
		minutos = calendario.get(Calendar.MINUTE);
		segundos = calendario.get(Calendar.SECOND);
		
		hora_actual=hora + ":" + minutos + ":" + segundos;
		
		db.setAutoCommit(false);
		
		SimpleDateFormat sdformat = new SimpleDateFormat("yyyy-MM-dd");
		String fechaActual= sdformat.format(new Date());
		
		String sistemaOperativo = System.getProperty("os.name");
		String file;
		  
		if(sistemaOperativo.equals("Windows 7") || sistemaOperativo.equals("Windows 8") || sistemaOperativo.equals("Windows 10")) 
			file =  "C:/log/logActualizarDiasVencidos_"+fechaActual+".txt";
		else
			file = "./logActualizarDiasVencidos_"+fechaActual+".txt";
		
		File newLogFile = new File(file);
		FileWriter fw;
		String str="";
		
		if(!newLogFile.exists())
			fw = new FileWriter(newLogFile);
		else
			fw = new FileWriter(newLogFile,true);
		
		BufferedWriter bw=new BufferedWriter(fw);
		
		Timestamp timestamp = new Timestamp(System.currentTimeMillis());
		
		SimpleDateFormat date = new SimpleDateFormat("yyyy-MM-dd");
		Date parsed = date.parse(fechaActual);
        java.sql.Date sqlDate = new java.sql.Date(parsed.getTime());
		
        
        
		try
		{
			str = "----------"+timestamp+"----------";	
			bw.write(str);
			bw.flush();
			bw.newLine();
			bw.newLine();
			
			str = "-- INICIO DEL PROCESO: 'Actualizar los dias y saldos vencidos' --";	
			bw.write(str);
			bw.flush();
			bw.newLine();
			
			str = "<<Consultando clientes activos...";	
			bw.write(str);
			bw.flush();
			bw.newLine();
			
			sql="SELECT distinct(smn_comercial.smn_cliente.smn_cliente_id)"
					+ " FROM smn_comercial.smn_cliente"
					+ " INNER JOIN smn_cobranzas.smn_mov_documento_cob_cab ON smn_cobranzas.smn_mov_documento_cob_cab.smn_cliente_rf=smn_comercial.smn_cliente.smn_cliente_id"
					+ " WHERE cli_estatus='AC' AND (mdc_saldo_doc_ml<>0 OR mdc_saldo_doc_ma<>0)";
			Statement stmt = db.createStatement();
			ResultSet rsClientes=stmt.executeQuery(sql);
			
			
			
			while(rsClientes.next())
			{
				smn_cliente_rf=rsClientes.getInt("smn_cliente_id");
				
				
				str = "<<Consultando movimientos (cabecera) PENDIENTES del cliente ID"+smn_cliente_rf;	
				bw.write(str);
				bw.flush();
				bw.newLine();
				
				sql="SELECT smn_documento_id, mdc_fecha_vencimiento, smn_mov_documento_cob_cab_id, mdc_saldo_doc_ml, mdc_saldo_doc_ma, smn_entidad_rf, smn_sucursal_rf"
						+ " FROM smn_cobranzas.smn_mov_documento_cob_cab"
						+ " WHERE smn_cliente_rf="+smn_cliente_rf+" AND mdc_estatus_financiero='PE'";
				Statement stmt1 = db.createStatement();
				ResultSet rsMovDocCobCab=stmt1.executeQuery(sql);
				
				
				scl_saldo_vencido_ml=0;
				scl_saldo_vencido_ma=0;
				
				
				
				while(rsMovDocCobCab.next())
				{
					smn_mov_documento_cob_cab_id=rsMovDocCobCab.getInt("smn_mov_documento_cob_cab_id");
					
					str = "<<Calculando dias de vencimiento para el movimiento cabecera con ID "+smn_mov_documento_cob_cab_id;	
					bw.write(str);
					bw.flush();
					bw.newLine();
					
					mdc_fecha_vencimiento = date.parse(rsMovDocCobCab.getString("mdc_fecha_vencimiento"));
					
					// La fecha actual
					Date fechaactual = new Date(System.currentTimeMillis());
					milisecondsByDay = 86400000;
					
					mdc_dias_vencidos = (int) ((fechaactual.getTime()-mdc_fecha_vencimiento.getTime()) / milisecondsByDay);
					
					mdc_saldo_doc_ml=rsMovDocCobCab.getDouble("mdc_saldo_doc_ml");
					
					if(rsMovDocCobCab.getString("mdc_saldo_doc_ma")!=null)
						mdc_saldo_doc_ma=rsMovDocCobCab.getDouble("mdc_saldo_doc_ma");
					else
						mdc_saldo_doc_ma=0;
					
					if(rsMovDocCobCab.getString("smn_entidad_rf")!=null)
						smn_entidad_rf=rsMovDocCobCab.getInt("smn_entidad_rf");
					else
						smn_entidad_rf=0;
					
					if(rsMovDocCobCab.getString("smn_sucursal_rf")!=null)
						smn_sucursal_rf=rsMovDocCobCab.getInt("smn_sucursal_rf");
					else
						smn_sucursal_rf=0;
					
					if(mdc_dias_vencidos>0)
					{
						scl_saldo_vencido_ml+=mdc_saldo_doc_ml;
						scl_saldo_vencido_ma+=mdc_saldo_doc_ma;
					}
					
					total_dias_vencidos+=mdc_dias_vencidos;
					
					str = "<<Actualizando dias vencidos...";	
					bw.write(str);
					bw.flush();
					bw.newLine();
					
					
				    sql = "UPDATE smn_cobranzas.smn_mov_documento_cob_cab SET mdc_dias_vencidos = ? WHERE smn_mov_documento_cob_cab_id = ?";
				    PreparedStatement preparedStmt = db.prepareStatement(sql);
				    preparedStmt.setInt(1, mdc_dias_vencidos);
				    preparedStmt.setInt(2, smn_mov_documento_cob_cab_id);
				    preparedStmt.executeUpdate();
				    
				    str = ">>Dias vencidos actualizados";	
					bw.write(str);
					bw.flush();
					bw.newLine();
				} //END-WHILE rsMovDocCobCab
				
				str = "<<Consultando movimientos (detalle) del cliente ID"+smn_cliente_rf;	
				bw.write(str);
				bw.flush();
				bw.newLine();
				
				sql="SELECT smn_cobranzas.smn_mov_documento_cob_detalle.smn_cliente_rf,"+
					"SUM(smn_cobranzas.smn_mov_documento_cob_cab.mdc_dias_vencidos) as dias_vencidos,"+ 
					"SUM(smn_cobranzas.smn_mov_documento_cob_detalle.mdd_saldo_ml) as saldo_ml,"+ 
					"SUM(smn_cobranzas.smn_mov_documento_cob_detalle.mdd_saldo_ma) as saldo_ma, "+
					"COUNT(smn_cobranzas.smn_mov_documento_cob_cab.smn_mov_documento_cob_cab_id) as cant_docs, "+
					"(SUM(smn_cobranzas.smn_mov_documento_cob_cab.mdc_dias_vencidos)/COUNT(smn_cobranzas.smn_mov_documento_cob_cab.smn_mov_documento_cob_cab_id)) as promedio_dias_vencidos "+
					"FROM smn_cobranzas.smn_mov_documento_cob_detalle INNER JOIN "+
					"smn_cobranzas.smn_mov_documento_cob_cab ON "+ 
					"smn_cobranzas.smn_mov_documento_cob_detalle.smn_mov_documento_cob_cab_id = "+
					"smn_cobranzas.smn_mov_documento_cob_cab.smn_mov_documento_cob_cab_id"+ 
					" WHERE smn_cobranzas.smn_mov_documento_cob_cab.mdc_estatus_proceso = 'GE' AND "+
					"smn_cobranzas.smn_mov_documento_cob_cab.mdc_estatus_financiero = 'PE' AND "+
					"(current_date - smn_cobranzas.smn_mov_documento_cob_cab.mdc_fecha_vencimiento)>0 AND "+
					"smn_cobranzas.smn_mov_documento_cob_detalle.smn_cliente_rf="+smn_cliente_rf+
					"GROUP BY smn_cobranzas.smn_mov_documento_cob_detalle.smn_cliente_rf";
				Statement stmt2 = db.createStatement();
				ResultSet rsMovDocCobDet=stmt2.executeQuery(sql);
				
				scl_debitos_ml=0;
				scl_debitos_ma=0;
				scl_creditos_ml=0;
				scl_creditos_ma=0;
				cantidad_docs=0;
				scl_promedio_dias_vencido=0;
				scl_saldo_vencido_ml=0;
				scl_saldo_vencido_ma=0;
				
				while(rsMovDocCobDet.next())
				{
					mdc_dias_vencidos=rsMovDocCobDet.getInt("dias_vencidos");
					scl_saldo_vencido_ml=rsMovDocCobDet.getDouble("saldo_ml");
					scl_saldo_vencido_ma=rsMovDocCobDet.getDouble("saldo_ma");
					cantidad_docs = rsMovDocCobDet.getInt("cant_docs");
					scl_promedio_dias_vencido=rsMovDocCobDet.getDouble("promedio_dias_vencidos");
				}
				
				str = "<<Consultando si hay control de saldo del cliente...";	
				bw.write(str);
				bw.flush();
				bw.newLine();
				
				sql="SELECT smn_saldo_cliente_id, scl_saldo_final_ml, scl_saldo_final_ma, scl_fecha, smn_entidad_rf, smn_sucursal_rf"
						+ " FROM smn_cobranzas.smn_saldo_cliente"
						+ " WHERE smn_cliente_rf="+smn_cliente_rf
						+ " ORDER BY scl_fecha DESC LIMIT 1";
				Statement stmt4 = db.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, 
                        ResultSet.CONCUR_UPDATABLE);
				ResultSet rsSaldoCliente=stmt4.executeQuery(sql);
				
				scl_saldo_inicial_ml=0;
				scl_saldo_inicial_ma=0;
				smn_saldo_cliente_id=0;
				scl_fecha=null;
				
				while(rsSaldoCliente.next())
				{
					
					scl_saldo_inicial_ml=rsSaldoCliente.getDouble("scl_saldo_final_ml");
					scl_saldo_inicial_ma=rsSaldoCliente.getDouble("scl_saldo_final_ma");
					smn_saldo_cliente_id=rsSaldoCliente.getInt("smn_saldo_cliente_id");
					scl_fecha=rsSaldoCliente.getDate("scl_fecha");
					
					if(rsSaldoCliente.getString("smn_entidad_rf")!=null)
						smn_entidad_rf=rsSaldoCliente.getInt("smn_entidad_rf");
					else
						smn_entidad_rf=0;
					
					if(rsSaldoCliente.getString("smn_sucursal_rf")!=null)
						smn_sucursal_rf=rsSaldoCliente.getInt("smn_sucursal_rf");
					else
						smn_sucursal_rf=0;
				}
				
				scl_saldo_final_ml=scl_saldo_inicial_ml;
				scl_saldo_final_ma=scl_saldo_inicial_ma;
				
				
				
				if(scl_promedio_dias_vencido>0)
				{
					str = ">>Tiene documentos vencidos";	
					bw.write(str);
					bw.flush();
					bw.newLine();
					
					if(scl_promedio_dias_vencido<=7)
					{
						str = ">>Vencido recientemente";	
						bw.write(str);
						bw.flush();
						bw.newLine();
						scl_estatus="VR";
					}
					else
					if(scl_promedio_dias_vencido>7 && scl_promedio_dias_vencido<=30)
					{
						str = ">>Vencido";	
						bw.write(str);
						bw.flush();
						bw.newLine();
						scl_estatus="VE";
					}
					else
					if(scl_promedio_dias_vencido>30)
					{
						str = ">>En mora";	
						bw.write(str);
						bw.flush();
						bw.newLine();
						scl_estatus="EM";
					}				
					
//					scl_estatus="GE"; consultar esto
					
					str = "<<Calculando porcentaje de deuda vencida...";	
					bw.write(str);
					bw.flush();
					bw.newLine();
					
					scl_porcentaje_deuda_vencida=scl_saldo_vencido_ml / scl_saldo_final_ml * 100;
					scl_porcentaje_deuda_vencida_ma=scl_saldo_vencido_ma / scl_saldo_final_ma * 100;
				}
				else
				{
					str = "Esta solvente";	
					bw.write(str);
					bw.flush();
					bw.newLine();
					scl_estatus="SO";
					scl_saldo_vencido_ml=0;
					scl_saldo_vencido_ma=0;
					scl_porcentaje_deuda_vencida=0;
					scl_porcentaje_deuda_vencida_ma=0;
				}
				
				if(scl_fecha!=null)
				{
					str = "<<Registrando saldo del cliente...";
					bw.write(str);
					bw.flush();
					bw.newLine();
					
					sql = "INSERT INTO smn_cobranzas.smn_saldo_cliente(smn_saldo_cliente_id,smn_entidad_rf,smn_sucursal_rf,smn_cliente_rf,"
							+ "scl_fecha,scl_saldo_inicial_ml,scl_debitos_ml,scl_creditos_ml,"
							+ "scl_saldo_final_ml,scl_saldo_inicial_ma,scl_debitos_ma,scl_creditos_ma,"
							+ "scl_saldo_final_ma,scl_saldo_vencido_ml,scl_saldo_vencido_ma,scl_estatus,"
							+ "scl_monto_doc_registrados_ml,scl_monto_doc_registrados_ma,"
							+ "scl_porcentaje_deuda_vencida,scl_porcentaje_deuda_vencida_ma,scl_idioma,"
							+ "scl_usuario,scl_fecha_registro,scl_hora)VALUES(nextval('smn_cobranzas.seq_smn_saldo_cliente'),?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
					PreparedStatement preparedStmt1 = db.prepareStatement(sql);
					preparedStmt1.setInt(1, smn_entidad_rf);
					preparedStmt1.setInt(2, smn_sucursal_rf);
					preparedStmt1.setInt(3, smn_cliente_rf);
					preparedStmt1.setDate(4, sqlDate);
					preparedStmt1.setDouble(5, scl_saldo_inicial_ml);
					preparedStmt1.setDouble(6, scl_debitos_ml);
					preparedStmt1.setDouble(7, scl_creditos_ml);
					preparedStmt1.setDouble(8, scl_saldo_final_ml);
					preparedStmt1.setDouble(9, scl_saldo_inicial_ma);
					preparedStmt1.setDouble(10, scl_debitos_ma);
					preparedStmt1.setDouble(11, scl_creditos_ma);
					preparedStmt1.setDouble(12, scl_saldo_final_ma);
					preparedStmt1.setDouble(13, scl_saldo_vencido_ml);
					preparedStmt1.setDouble(14, scl_saldo_vencido_ma);
					preparedStmt1.setString(15, scl_estatus);
					preparedStmt1.setDouble(16, scl_monto_doc_registrados_ml);
					preparedStmt1.setDouble(17, scl_monto_doc_registrados_ma);
					preparedStmt1.setDouble(18, scl_porcentaje_deuda_vencida_ma);
					preparedStmt1.setDouble(19, scl_porcentaje_deuda_vencida_ma);
					preparedStmt1.setString(20, "es");
					preparedStmt1.setString(21, "admin");
					preparedStmt1.setDate(21, sqlDate);
					preparedStmt1.setString(23, hora_actual);
					preparedStmt1.execute();
					
					str = ">>Saldo del cliente registrado exitosamente";
					bw.write(str);
					bw.flush();
					bw.newLine();
					
					str = "<<Eliminando control de saldo con fecha "+scl_fecha;	
					bw.write(str);
					bw.flush();
					bw.newLine();
					
				   /* sql = "DELETE FROM smn_cobranzas.smn_saldo_cliente WHERE smn_saldo_cliente_id = ?";
				    PreparedStatement preparedStmt2 = db.prepareStatement(sql);
				    preparedStmt2.setInt(1, smn_saldo_cliente_id);
				    preparedStmt2.executeUpdate();*/
				    
				    str = ">>Control de saldo eliminado";	
					bw.write(str);
					bw.flush();
					bw.newLine();
					result=0;
				}
				else
				{
					str = "<<Registrando saldo del cliente...";
					bw.write(str);
					bw.flush();
					bw.newLine();
					
					sql = "INSERT INTO smn_cobranzas.smn_saldo_cliente(smn_saldo_cliente_id,smn_entidad_rf,smn_sucursal_rf,smn_cliente_rf,scl_fecha,scl_saldo_inicial_ml,scl_debitos_ml,scl_creditos_ml,scl_saldo_final_ml,scl_saldo_inicial_ma,scl_debitos_ma,scl_creditos_ma,scl_saldo_final_ma,scl_saldo_vencido_ml,scl_saldo_vencido_ma,scl_estatus,scl_monto_doc_registrados_ml,scl_monto_doc_registrados_ma,scl_porcentaje_deuda_vencida,scl_porcentaje_deuda_vencida_ma,scl_idioma,scl_usuario,scl_fecha_registro,scl_hora)VALUES(nextval('smn_cobranzas.seq_smn_saldo_cliente'),?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
					PreparedStatement preparedStmt1 = db.prepareStatement(sql);
					preparedStmt1.setInt(1, smn_entidad_rf);
					preparedStmt1.setInt(2, smn_sucursal_rf);
					preparedStmt1.setInt(3, smn_cliente_rf);
					preparedStmt1.setDate(4, sqlDate);
					preparedStmt1.setDouble(5, scl_saldo_inicial_ml);
					preparedStmt1.setDouble(6, scl_debitos_ml);
					preparedStmt1.setDouble(7, scl_creditos_ml);
					preparedStmt1.setDouble(8, scl_saldo_final_ml);
					preparedStmt1.setDouble(9, scl_saldo_inicial_ma);
					preparedStmt1.setDouble(10, scl_debitos_ma);
					preparedStmt1.setDouble(11, scl_creditos_ma);
					preparedStmt1.setDouble(12, scl_saldo_final_ma);
					preparedStmt1.setDouble(13, scl_saldo_vencido_ml);
					preparedStmt1.setDouble(14, scl_saldo_vencido_ma);
					preparedStmt1.setString(15, scl_estatus);
					preparedStmt1.setDouble(16, scl_monto_doc_registrados_ml);
					preparedStmt1.setDouble(17, scl_monto_doc_registrados_ma);
					preparedStmt1.setDouble(18, scl_porcentaje_deuda_vencida_ma);
					preparedStmt1.setDouble(19, scl_porcentaje_deuda_vencida_ma);
					preparedStmt1.setString(20, "es");
					preparedStmt1.setString(21, "admin");
					preparedStmt1.setDate(21, sqlDate);
					preparedStmt1.setString(23, hora_actual);
					preparedStmt1.execute();
					
					str = ">>Saldo del cliente registrado exitosamente";
					bw.write(str);
					bw.flush();
					bw.newLine();
					result=0;
				}
			} //END-WHILE rsClientes
		}
		catch(Throwable e)
		{
			db.rollback();
			throw e;
		}
		finally
		{
			if(result == 0)
			{
				db.commit();
				str = "Cambios efectuados en la base de datos";	
				bw.write(str);
				bw.flush();
				bw.newLine();
				
				/*if(sistemaOperativo.equals("Windows 7") || sistemaOperativo.equals("Windows 8") || sistemaOperativo.equals("Windows 10")) 
					cmd_command="cmd /c start cmd.exe /k \" msg %username% Programa de actualizar dias vencidos ejecutado exitosamente && exit";
				else
					cmd_command="zenity *notification *text Programa de actualizar dias vencidos ejecutado exitosamente && exit";*/
			}
			else
			{
				db.rollback();
				str = "Cambios no efectuados en la base de datos";	
				bw.write(str);
				bw.flush();
				bw.newLine();
				
				/*if(sistemaOperativo.equals("Windows 7") || sistemaOperativo.equals("Windows 8") || sistemaOperativo.equals("Windows 10")) 
					cmd_command="cmd /c start cmd.exe /k \" msg %username% Ocurrio un error en el Programa de actualizar dias vencidos, revisar log: logActualizarDiasVencidos_"+fechaActual+".txt && exit";
				else
					cmd_command="zenity --error --text Ocurrio un error en el Programa de actualizar dias vencidos, revisar log: logActualizarDiasVencidos_"+fechaActual+".txt && exit";*/
			}
		
			//Runtime.getRuntime().exec(cmd_command);
			
			if(db!=null)
				db.close();
			
			str = "FINAL DEL PROCESO";	
			bw.write(str);
			bw.flush();
			bw.newLine();
			
			bw.close();
		}
	}
}
