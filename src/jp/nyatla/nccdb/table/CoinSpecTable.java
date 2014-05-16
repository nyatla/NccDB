package jp.nyatla.nccdb.table;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import jp.nyatla.nccdb.table.CoinMasterTable.Item;
import jp.nyatla.nyansat.db.basic.BasicTableDefinition;
import jp.nyatla.nyansat.db.basic.InsertSqlBuilder;
import jp.nyatla.nyansat.db.basic.PsUtils;
import jp.nyatla.nyansat.db.basic.RsUtils;
import jp.nyatla.nyansat.db.basic.SqliteDB;
import jp.nyatla.nyansat.db.basic.table.BaseTable;
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
public class CoinSpecTable extends BaseTable<CoinSpecTable.Item>
{
	public final static String NAME="coin_spec";
	public final static String DN_id="id";
	public final static String DN_total_coin="total_coin";
	public final static String DN_premine="premine";
	public final static String DN_id_algorism="id_coin_algorism";
	private static class TableDef extends BasicTableDefinition<Item>
	{
		private String[] _cols={
				DN_id,DN_total_coin,DN_premine,DN_id_algorism};
		public TableDef(String i_name) {
			super(i_name);
		}
		@Override
		public String getCreateStr(){
			return "("+
					DN_id+" integer,"+
					DN_total_coin+" real,"+
					DN_premine+" real,"+
					DN_id_algorism+" integer,"+
					"primary key("+DN_id+"),unique("+DN_total_coin+","+DN_premine+","+DN_id_algorism+"))";
		}
		@Override
		public String[] getElementNames() {
			return this._cols;
		}
		@Override
		public Item createRowItem(ResultSet rs) throws SdbException {
			return new Item(rs);
		}		
	}	


	@Override
	public void dispose() throws SdbException
	{
		try {
			if(this._ps_insert!=null){
				this._ps_insert.close();
			}
			this._ps_update.close();
			this._ps_delete_id.close();
		} catch (SQLException e) {
			throw new SdbException(e);
		}
	}

	private PreparedStatement _ps_insert;
	private PreparedStatement _ps_search_id;
	private PreparedStatement _ps_update;
	private PreparedStatement _ps_delete_id;
	
	public CoinSpecTable(SqliteDB i_db) throws SdbException
	{
		this(i_db,NAME);
	}	
	public CoinSpecTable(SqliteDB i_db,String i_table_name) throws SdbException
	{
		super(i_db,new TableDef(i_table_name));
		try {
			String table_name=this._table_info.getTableName();
			this._ps_search_id=this._db.getConnection().prepareStatement(
				String.format("SELECT * FROM %s WHERE %s=?;",table_name,DN_id));
			this._ps_update=this._db.getConnection().prepareStatement(
				String.format("UPDATE %s SET %s=?,%s=?,%s=? WHERE %s=?;",table_name,DN_total_coin,DN_premine,DN_id_algorism,DN_id));
			this._ps_delete_id=this._db.getConnection().prepareStatement(
				String.format("DELETE FROM %s WHERE %s=?;",table_name,DN_id));
			
		} catch (SQLException e) {
			throw new SdbException(e);
		}
	}
	private final static int COL_ID			=0x80000000;
	private final static int COL_TOTAL_COIN	=0x40000000;
	private final static int COL_PREMINE	=0x20000000;
	private final static int COL_ALGORISM	=0x10000000;
	private int _add_last_flags=0;
	private boolean add(Item i_item,int i_flags) throws SdbException
	{
		try {
			//必要ならpreparedStatementを作り直す。
			if(this._add_last_flags!=i_flags){
				//PreparedStatementの再構築
				InsertSqlBuilder isb=new InsertSqlBuilder();
				isb.init(this._table_info.getTableName());
				if((i_flags&COL_ID)!=0){
					isb.add(DN_id);
				}
				if((i_flags&COL_TOTAL_COIN)!=0){
					isb.add(DN_total_coin);
				}
				if((i_flags&COL_PREMINE)!=0){
					isb.add(DN_premine);
				}
				if((i_flags&COL_ALGORISM)!=0){
					isb.add(DN_id_algorism);
				}
				this._ps_insert=this._db.getConnection().prepareStatement(isb.finish());
				this._add_last_flags=i_flags;
			}
			//PreperedStatementの実行
			int idx=0;
			if((i_flags&COL_ID)!=0){
				this._ps_insert.setInt(++idx,i_item.id);
			}
			if((i_flags&COL_TOTAL_COIN)!=0){
				PsUtils.setNullableDouble(this._ps_insert,++idx,i_item.total_coin);
			}
			if((i_flags&COL_PREMINE)!=0){
				PsUtils.setNullableDouble(this._ps_insert,++idx,i_item.premine);
			}
			if((i_flags&COL_ALGORISM)!=0){
				this._ps_insert.setInt(++idx,i_item.id_coin_algorism);
			}
			this._ps_insert.execute();
			boolean r=this._ps_insert.getUpdateCount()>0;
			return r;
		} catch (SQLException e){
			throw new SdbException(e);
		}
	}	

	public boolean add(Double i_total_coin,Double i_premine,int i_id_algorism) throws SdbException
	{
		return this.add(
			new Item(null,i_total_coin,i_premine,i_id_algorism),
			COL_TOTAL_COIN|COL_PREMINE|COL_ALGORISM);
	}


	public Item getItem(Double i_total_coin,Double i_premine,int i_algolism) throws SdbException
	{
		try{
			ResultSet rs=null;
			Item result=null;
			PreparedStatement ps=null;
			try{
				String sql="select * from "+this._table_info.getTableName()+" where "
					+DN_total_coin+((i_total_coin==null)?" is null AND ":"=? AND ")
					+DN_premine+((i_premine==null)?" is null AND ":"=? AND ")
					+DN_id_algorism+"=?;";
				ps=this._db.getConnection().prepareStatement(sql);
				int idx=0;
				if(i_total_coin!=null){
					PsUtils.setNullableDouble(ps,++idx,i_total_coin);
				}
				if(i_premine!=null){
					PsUtils.setNullableDouble(ps,++idx,i_premine);
				}
				PsUtils.setNullableInt(ps,++idx,i_algolism);
				rs=ps.executeQuery();
				if(rs.next()){
					result=new Item(
						rs.getInt(DN_id),
						RsUtils.getNullableDouble(rs,DN_total_coin),
						RsUtils.getNullableDouble(rs,DN_premine),
						rs.getInt(DN_id_algorism));
				}
			}finally{
				if(rs!=null){
					rs.close();
				}
				if(ps!=null){
					ps.close();
				}
			}
			return result;
		}catch(SQLException e){
			throw new SdbException(e);
		}
	}	

	public Item getItem(int i_id) throws SdbException
	{
		try{
			ResultSet rs=null;
			Item result=null;
			try{
				this._ps_search_id.setInt(1,i_id);
				rs=this._ps_search_id.executeQuery();
				if(rs.next()){
					result=new Item(
						rs.getInt(DN_id),
						RsUtils.getNullableDouble(rs,DN_total_coin),
						RsUtils.getNullableDouble(rs,DN_premine),
						rs.getInt(DN_id_algorism));
				}
			}finally{
				if(rs!=null){
					rs.close();
				}
			}
			return result;
		}catch(SQLException e){
			throw new SdbException(e);
		}
	}
	public boolean deleteItem(int i_id) throws SdbException
	{
		try{
			this._ps_delete_id.setInt(1,i_id);
			int ret=this._ps_delete_id.executeUpdate();
			return ret>0;
		}catch(SQLException e){
			throw new SdbException(e);
		}		
	}
	
	public static boolean isNullableEqual(Double i_1,Double i_2)
	{
		if(i_1!=null){
			return i_1.equals(i_2);
		}
		return i_2==null;
	}
	public static boolean isNullableEqual(Integer i_1,Integer i_2)
	{
		if(i_1!=null){
			return i_1.equals(i_2);
		}
		return i_2==null;
	}
	public static class Item
	{
		public Integer id;
		public Double total_coin;
		public Double premine;
		public Integer id_coin_algorism;
		public Item(Integer i_id,Double i_total_coin,Double i_premine,Integer i_id_coin_algorism)
		{
			this.id=i_id;
			this.total_coin=i_total_coin;
			this.premine=i_premine;
			this.id_coin_algorism=i_id_coin_algorism;
		}
		public Item(ResultSet i_rs) throws SdbException
		{
			try{
				this.id=i_rs.getInt(DN_id);
				this.total_coin			=RsUtils.getNullableDouble(i_rs,DN_total_coin);
				this.premine			=RsUtils.getNullableDouble(i_rs,DN_premine);
				this.id_coin_algorism	=RsUtils.getNullableInt(i_rs,DN_id_algorism);
			} catch (SQLException e){
				throw new SdbException(e);
			}
		}
		
		public boolean match(Double i_spec_total, Double i_spec_premine,int i_spec_algorism)
		{
			return 		isNullableEqual(this.total_coin,i_spec_total)
					&&	isNullableEqual(this.premine,i_spec_premine)
					&&	(this.id_coin_algorism.intValue()==i_spec_algorism);
		}
		
	}

}