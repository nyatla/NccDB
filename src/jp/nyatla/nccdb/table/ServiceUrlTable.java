package jp.nyatla.nccdb.table;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import jp.nyatla.nccdb.table.internal.ServiceTypeTable;
import jp.nyatla.nyansat.db.basic.RowIterable;
import jp.nyatla.nyansat.db.basic.BasicTableDefinition;
import jp.nyatla.nyansat.db.basic.PsUtils;
import jp.nyatla.nyansat.db.basic.RsUtils;
import jp.nyatla.nyansat.db.basic.SqliteDB;
import jp.nyatla.nyansat.db.basic.UpdateSqlBuilder;
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
public class ServiceUrlTable extends BaseTable<ServiceUrlTable.Item>
{
	public final static String NAME="service_url";
	public final static String DN_id="id";
	public final static String DN_name="name";
	public final static String DN_id_coin_url_status="id_coin_url_status";
	public final static String DN_url="url";	
	public final static String DN_description="description";	

	private static class TableDef extends BasicTableDefinition<Item>
	{
		private String[] _cols={
				DN_id,DN_name,DN_id_coin_url_status,DN_url,DN_description};
		public TableDef(String i_name) {
			super(i_name);
		}
		@Override
		public String getCreateStr(){
			return "("+
				DN_id+" integer,"+
				DN_name+" text,"+
				DN_id_coin_url_status+" integer,"+
				DN_url+" text,"+
				DN_description+" text,"+
				"primary key("+DN_id+"),"+
				"unique("+DN_url+"))";
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

	public String[] getColHeader()
	{
		return this._table_info.getElementNames();
	}	
	@Override
	public void dispose() throws SdbException
	{
		try {
			this._ps_select_by_name_type.close();
			this._ps_select_by_url.close();
			this._ps_insert.close();
			this._ps_select_all_asc.close();
			this._ps_update.close();
		} catch (SQLException e) {
			throw new SdbException(e);
		}
	}

	private PreparedStatement _ps_insert;
	private PreparedStatement _ps_select_all_asc;
	private PreparedStatement _ps_update;
	private PreparedStatement _ps_select_by_name_type;
	private PreparedStatement _ps_select_by_url;
	public ServiceUrlTable(SqliteDB i_db) throws SdbException
	{
		this(i_db,NAME);
	}
	public ServiceUrlTable(SqliteDB i_db,String i_table_name) throws SdbException
	{
		super(i_db,new TableDef(i_table_name));
		try {
			String table_name=this._table_info.getTableName();
			this._ps_insert=this._db.getConnection().prepareStatement("insert or ignore into "+table_name+
				"("+DN_name+","+DN_id_coin_url_status+","+DN_url+","+DN_description+") values(?,?,?,?);");
			this._ps_select_all_asc=this._db.getConnection().prepareStatement(
				String.format("select * from %s ORDER BY %s ASC;", table_name,DN_name));
			this._ps_select_by_name_type=this._db.getConnection().prepareStatement(
				String.format("select * from %s where %s=?;",table_name,DN_name));
			this._ps_select_by_url=this._db.getConnection().prepareStatement(
				String.format("select * from %s where %s=?;",table_name,DN_url));
			UpdateSqlBuilder usb=new UpdateSqlBuilder();
			//nameをキーとしたupdateの生成
			usb.init(table_name);
			usb.add(new String[]{DN_name,DN_id_coin_url_status,DN_url,DN_description});
			this._ps_update=this._db.getConnection().prepareStatement(usb.finish(DN_url+"=?"));
		} catch (SQLException e){
			throw new SdbException(e);
		}
	}
	protected boolean add(String i_name,int id_coin_url_status,String i_url,String i_description) throws SdbException
	{
		try {
			if(i_url==null){
				throw new SdbException();
			}
			PsUtils.setNullableString(this._ps_insert,1,i_name);
			this._ps_insert.setInt(2,id_coin_url_status);
			this._ps_insert.setString(3,i_url);
			PsUtils.setNullableString(this._ps_insert,4,i_description);
			this._ps_insert.execute();
			return this._ps_insert.getUpdateCount()>0;
		} catch (SQLException e) {
			throw new SdbException(e);
		}
	}
	/**
	 * データを更新する。検索キーはi_id_coin_url_typeとi_url.
	 * 該当データが存在しない場合は{@link #add}を実行する。
	 * @param i_name
	 * @param i_id_coin_url_type
	 * @param id_coin_url_status
	 * @param i_url
	 * @param i_description
	 * @return
	 * @throws SdbException
	 */
	public boolean update(String i_name,int id_coin_url_status,String i_url,String i_description) throws SdbException
	{
		try {
			if(i_url==null){
				throw new SdbException();
			}
			PsUtils.setNullableString(this._ps_update,1,i_name);
			this._ps_update.setInt(2,id_coin_url_status);
			this._ps_update.setString(3,i_url);
			PsUtils.setNullableString(this._ps_update,4,i_description);
			this._ps_update.setString(5,i_url);
			this._ps_update.execute();
			boolean r=this._ps_update.getUpdateCount()>0;
			if(!r){
				return this.add(i_name,id_coin_url_status,i_url,i_description);
			}else{
				return true;
			}
		} catch (SQLException e) {
			throw new SdbException(e);
		}
	}
	
	public static class Item
	{
		public Item(ResultSet i_rs) throws SdbException
		{
			try{
				this.id=i_rs.getInt(DN_id);
				this.name=i_rs.getString(DN_name);
				this.id_coin_url_status=RsUtils.getNullableInt(i_rs,DN_id_coin_url_status);
				this.url=i_rs.getString(DN_url);
				this.description=i_rs.getString(DN_description);
			}catch(SQLException e){
				throw new SdbException(e);
			}
		}
		public int id;
		public String name;
		public int id_coin_url_status;
		public String url;
		public String description;
		public String[] toCsvArrray()
		{
			return new String[]{
				Integer.toString(this.id),
				this.name,
				Integer.toString(this.id_coin_url_status),
				this.url==null?"":this.url,
				this.description==null?"":this.description
				};
		}
	}
	public RowIterable<Item> getAllAsc() throws SdbException
	{
		try {
			ResultSet rs=this._ps_select_all_asc.executeQuery();
			return new RowIterable<Item>(rs,this._table_info);
		} catch (SQLException e) {
			throw new SdbException(e);
		}
	}/*
	public Item getItemByNameType(String i_service_name,int i_url_type) throws SdbException
	{
		try{
			ResultSet rs=null;
			try{
				this._ps_select_by_name_type.setString(1,i_service_name);
				this._ps_select_by_name_type.setInt(2,i_url_type);
				rs=this._ps_select_by_name_type.executeQuery();
				if(rs.next()){
					return new Item(rs);
				}else{
					return null;
				}
			}finally{
				if(rs!=null){
					rs.close();
				}
			}
		}catch(SQLException e){
			throw new SdbException(e);
		}
	}*/
	public Item getItemByUrl(String i_url) throws SdbException
	{
		try{
			ResultSet rs=null;
			try{
				this._ps_select_by_url.setString(1,i_url);
				rs=this._ps_select_by_url.executeQuery();
				if(rs.next()){
					return new Item(rs);
				}else{
					return null;
				}
			}finally{
				if(rs!=null){
					rs.close();
				}
			}
		}catch(SQLException e){
			throw new SdbException(e);
		}
	}
/*
	public final class RowIterable extends BaseRowIterable<Item>
	{
		public RowIterable(ResultSet i_rs)
		{
			super(i_rs);
		}

		@Override
		protected Item createItem(ResultSet i_rs) throws SdbException
		{
			try {
				return new Item(i_rs);
			} catch (SQLException e) {
				return null;
			}
		}
	}*/

}