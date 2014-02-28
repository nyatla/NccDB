package jp.nyatla.nccdb.table;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;

import jp.nyatla.nyansat.db.basic.table.BaseTable;
import jp.nyatla.nyansat.db.basic.table.SqliteDB;
import jp.nyatla.nyansat.utils.SdbException;

/**
 * コインのスペック情報を格納するテーブル。
 * <h2>目的</h2>
 * <h2>API</h2>
 * 
 * <h2>カラム構成</h2>
 * <p>
 * <ul>
 * <li>coin_symbol text - コインのシンボル名 (PK)
 * <li>coin_name text - プロダクト名
 * </ul>
 * unique(cpu_id,processor_number)
 * </p>
 */
public class CoinSpecTable extends BaseTable
{
	public final static String NAME="coin_spec";
	public final static String DN_id_coin_title="id_coin_title";
	public final static String DN_start_date="start_date";
	public final static String DN_total_coin="total_coin";
	public final static String DN_premine="premine";
	public final static String DN_id_block_reword_type="id_block_reword_type";	
	public final static String DN_id_algorism="id_coin_algorism";
/*
			int coin_start_date_idx=csv.hasIndex(DN_start_date)?csv.getIndex(DN_start_date):-1;
			if(i_start_date!=null){
				this._ps_insert.setLong(3,i_start_date);
			}else{
				this._ps_insert.setNull(3,java.sql.Types.INTEGER);
			}
			coin_start_date_idx>0?DateFormat.getDateInstance().parse(l[coin_start_date_idx]).getTime():0
 */
	
	protected String createTableDefinisitonStr()
	{
		return "("+
			DN_id_coin_title+" integer,"+
			DN_start_date+" integer,"+
			DN_total_coin+" real,"+
			DN_premine+" real,"+
			DN_id_block_reword_type+" integer,"+
			DN_id_algorism+" integer,"+
			"unique("+DN_id_coin_title+"))";
	}
	@Override
	public void dispose() throws SdbException
	{
		try {
			this._ps_insert.close();
			this._ps_update.close();
			this._ps_delete.close();
		} catch (SQLException e) {
			throw new SdbException(e);
		}
	}

	private PreparedStatement _ps_insert;
	private PreparedStatement _ps_delete;
	private PreparedStatement _ps_search_id;
	private PreparedStatement _ps_update;	
	public CoinSpecTable(SqliteDB i_db) throws SdbException
	{
		this(i_db,NAME);
	}	
	public CoinSpecTable(SqliteDB i_db,String i_table_name) throws SdbException
	{
		super(i_db,i_table_name);
		try {
			this._ps_insert=this._db.getConnection().prepareStatement("insert or ignore into "+this._tbl_name+
				"("+DN_id_coin_title+
				","+DN_start_date+
				","+DN_total_coin+
				","+DN_premine+
				","+DN_id_block_reword_type+
				","+DN_id_algorism+") values(?,?,?,?,?,?);");
			this._ps_search_id=this._db.getConnection().prepareStatement("select * from "+this._tbl_name +
					" where "+DN_id_coin_title+"=?;");
			this._ps_update=this._db.getConnection().prepareStatement("update "+this._tbl_name+" set "
					+DN_start_date+"=?,"
					+DN_total_coin+"=?,"
					+DN_premine+"=?,"
					+DN_id_block_reword_type+"=?,"
					+DN_id_algorism+"=? where "+DN_id_coin_title+"=?;");
			this._ps_delete=this._db.getConnection().prepareStatement("delete from "+this._tbl_name+" where "+DN_id_coin_title+"=?;");
			
		} catch (SQLException e) {
			throw new SdbException(e);
		}
	}
	public boolean update(int i_id_coin_title,Long i_start_date,Double i_total_coin,Double i_premine,int id_block_reword_type,int i_id_algorism) throws SdbException
	{
		Item item=this.getItem(i_id_coin_title);
		if(item!=null){
			//存在するなら行を更新
			try {
				if(i_start_date==null){
					this._ps_update.setNull(1,java.sql.Types.INTEGER);
				}else{
					this._ps_update.setLong(1,i_start_date);
				}
				if(i_total_coin==null){
					this._ps_update.setNull(2,java.sql.Types.DOUBLE);
				}else{
					this._ps_update.setDouble(2,i_total_coin);
				}
				if(i_premine==null){
					this._ps_update.setNull(3,java.sql.Types.DOUBLE);
				}else{
					this._ps_update.setDouble(3,i_premine);
				}
				this._ps_update.setInt(4,id_block_reword_type);
				this._ps_update.setInt(5,i_id_algorism);
				this._ps_update.setInt(6,i_id_coin_title);
				return this._ps_update.executeUpdate()>0;
			} catch (SQLException e){
				throw new SdbException(e);
			}
		}else{
			return this.add(i_id_coin_title,i_start_date,i_total_coin,i_premine,id_block_reword_type,i_id_algorism);
		}
	}	
	public boolean add(int i_id_coin_title,Long i_start_date,Double i_total_coin,Double i_premine,int id_block_reword_type,int i_id_algorism) throws SdbException
	{
		try {
			this._ps_insert.setInt(1,i_id_coin_title);
			if(i_start_date==null){
				this._ps_insert.setNull(2,java.sql.Types.INTEGER);
			}else{
				this._ps_insert.setLong(2,i_start_date);
			}
			if(i_premine==null){
				this._ps_insert.setNull(3,java.sql.Types.DOUBLE);
			}else{
				this._ps_insert.setDouble(3,i_total_coin);
			}
			if(i_premine==null){
				this._ps_insert.setNull(4,java.sql.Types.DOUBLE);
			}else{
				this._ps_insert.setDouble(4,i_premine);
			}
			this._ps_insert.setInt(5,id_block_reword_type);
			this._ps_insert.setInt(6,i_id_algorism);
			this._ps_insert.execute();
			return this._ps_insert.getUpdateCount()>0;
		} catch (SQLException e) {
			throw new SdbException(e);
		}
	}
	public Item getItem(int i_id_coin_title) throws SdbException
	{
		try{
			PreparedStatement s=null;
			ResultSet rs=null;
			Item result=new Item();
			try{
				this._ps_search_id.setInt(1,i_id_coin_title);
				rs=this._ps_search_id.executeQuery();
				if(rs.next()){
					result.id_coin_title=rs.getInt(DN_id_coin_title);
					result.total_coin=rs.getDouble(DN_total_coin);
					result.premine=rs.getDouble(DN_premine);
					if(rs.wasNull()){
						result.premine=null;
					}
					result.id_block_reword_type=rs.getInt(DN_id_block_reword_type);
					result.id_coin_algorism=rs.getInt(DN_id_algorism);
					result.start_date=rs.getLong(DN_start_date);
					if(rs.wasNull()){
						result.start_date=null;
					}

				}else{
					result=null;
				}
			}finally{
				if(rs!=null){
					rs.close();
				}
				if(s!=null){
					s.close();
				}
			}
			return result;
		}catch(SQLException e){
			throw new SdbException(e);
		}
	}	
	public static class Item
	{
		public int id_coin_title;
		public Long start_date;
		public double total_coin;
		public Double premine;
		public int id_block_reword_type;
		public int id_coin_algorism;
		
	}
}