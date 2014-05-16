package jp.nyatla.nccdb.table;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import jp.nyatla.nyansat.db.basic.RowIterable;
import jp.nyatla.nyansat.db.basic.BasicTableDefinition;
import jp.nyatla.nyansat.db.basic.InsertSqlBuilder;
import jp.nyatla.nyansat.db.basic.PsUtils;
import jp.nyatla.nyansat.db.basic.RsUtils;
import jp.nyatla.nyansat.db.basic.SqliteDB;
import jp.nyatla.nyansat.db.basic.UpdateSqlBuilder;
import jp.nyatla.nyansat.db.basic.table.BaseTable;
import jp.nyatla.nyansat.utils.SdbException;

/**
 * コインシンボルと名前のテーブル。
 * <h2>目的</h2>
 * CPUのプロダクト名を保存するテーブル.
 * <h2>API</h2>
 * CSVファイルからのインポート機能、検索機能
 * 
 * <h2>カラム構成</h2>
 * <p>
 * <ul>
 * <li>id int - コインID (PK)
 * <li>symbol text - シンボル名(UNQ)
 * <li>name text - コイン名（フルネーム）
 * <li>start_date int64 - コイン開始日
 * <li>alias_id int - 別名の場合のオリジナルのID
 * </ul>
 * unique(cpu_id,processor_number)
 * </p>
 */
public class CoinMasterTable extends BaseTable<CoinMasterTable.Item>
{
	public final static String NAME="coin_master";
	public final static String DN_id="id";
	public final static String DN_symbol="symbol";
	public final static String DN_name="name";
	public final static String DN_alias_id="alias_id";
	public final static String DN_start_date="start_date";
	public final static String DN_spec_id="spec_id";
	public final static String DN_comment="comment";

	private static class TableDef extends BasicTableDefinition<Item>
	{
		private String[] _cols={
				DN_id,DN_symbol,DN_name,DN_alias_id,DN_start_date,DN_spec_id,DN_comment};
		public TableDef(String i_name) {
			super(i_name);
		}
		@Override
		public String getCreateStr(){
			return "("+
					DN_id+" integer,"+
					DN_symbol+" text,"+
					DN_name+" text,"+
					DN_alias_id+" integer,"+
					DN_start_date+" integer,"+
					DN_spec_id+" integer,"+
					DN_comment+" text,"+
					"unique("+DN_symbol+","+DN_name+"),"+
					"primary key("+DN_id+"))";
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
			if(this._ps_update!=null){
				this._ps_update.close();
			}
			if(this._ps_insert!=null){
				this._ps_insert.close();
			}
			this._ps_search_symbol.close();
			this._ps_select_all_alias.close();
			this._ps_select_item_id.close();
			this._ps_select_all_spec.close();
		} catch (SQLException e) {
			throw new SdbException(e);
		}
	}


	private PreparedStatement _ps_insert;
	private PreparedStatement _ps_update;
	private PreparedStatement _ps_search_symbol;
	private PreparedStatement _ps_select_all_asc;
	private PreparedStatement _ps_select_all_alias;
	private PreparedStatement _ps_select_item_id;
	private PreparedStatement _ps_select_all_spec;
	
	public CoinMasterTable(SqliteDB i_db) throws SdbException
	{
		this(i_db,NAME);
	}
	public CoinMasterTable(SqliteDB i_db,String i_table_name) throws SdbException
	{
		super(i_db,new TableDef(i_table_name));
		try{
			String table_name=this._table_info.getTableName();
			this._ps_search_symbol=this._db.getConnection().prepareStatement(
					String.format("SELECT * FROM %s WHERE %s=? AND %s=?;",table_name,DN_symbol,DN_name));
			this._ps_select_all_asc=this._db.getConnection().prepareStatement(
					String.format("SELECT * FROM %s ORDER BY %s ASC;",table_name,CoinMasterTable.DN_symbol));
			this._ps_select_all_alias=this._db.getConnection().prepareStatement(
					String.format("SELECT * FROM %s WHERE %s IS NOT NULL;",table_name,CoinMasterTable.DN_alias_id));
			this._ps_select_item_id=this._db.getConnection().prepareStatement(
					String.format("SELECT * FROM %s WHERE %s=?;",table_name,CoinMasterTable.DN_id));
			this._ps_select_all_spec=this._db.getConnection().prepareStatement(
				String.format("SELECT * FROM %s WHERE %s=?;",table_name,CoinMasterTable.DN_spec_id));
		} catch (SQLException e) {
			throw new SdbException(e);
		}
	}
	/**
	 * 入力データを優先してデータを更新する。
	 * 更新項目はsymbol,name,alias_id,comment
	 * @param i_symbol
	 * @param i_coin_name
	 * @return
	 * @throws SdbException
	 */
	public boolean update(String i_symbol,String i_coin_name,Integer i_alias_id,Long i_start_date,Integer i_spec_id,String i_comment) throws SdbException
	{
		return this.update(
			i_symbol,i_coin_name,
			new Item(null,i_symbol,i_coin_name,i_alias_id,i_start_date,i_spec_id,i_comment),
			COL_SYMBOL|COL_NAME|COL_ALIAS|COL_START_DATE|COL_SPEC_ID|COL_COMMENT);
	}
	public boolean add(String i_symbol,String i_coin_name,Integer i_alias_id,Long i_start_date,Integer i_spec_id) throws SdbException {
		return this.add(
			new Item(null,i_symbol,i_coin_name,i_alias_id,i_start_date,i_spec_id,null),
			COL_SYMBOL|COL_NAME|COL_ALIAS|COL_START_DATE|COL_SPEC_ID);
	}	
	public boolean update(String i_symbol,String i_coin_name,Integer i_alias_id,Long i_start_date,Integer i_spec_id) throws SdbException
	{
		return this.update(
			i_symbol,i_coin_name,
			new Item(null,i_symbol,i_coin_name,i_alias_id,i_start_date,i_spec_id,null),
			COL_SYMBOL|COL_NAME|COL_ALIAS|COL_START_DATE|COL_SPEC_ID);
	}
	public boolean add(String i_symbol,String i_coin_name,Integer i_alias_id,Long i_start_date,Integer i_spec_id,String i_comment) throws SdbException
	{
		return this.add(
			new Item(null,i_symbol,i_coin_name,i_alias_id,i_start_date,i_spec_id,i_comment),
			COL_SYMBOL|COL_NAME|COL_ALIAS|COL_START_DATE|COL_SPEC_ID|COL_COMMENT);
	}
	public boolean updateAliasId(String i_symbol,String i_coin_name,Integer i_alias_id) throws SdbException
	{
		return this.update(
			i_symbol,i_coin_name,new Item(null,i_symbol,i_coin_name,i_alias_id,null,null,null),COL_ALIAS);
	}	
	private final static int COL_ID			=0x80000000;
	private final static int COL_SYMBOL		=0x40000000;
	private final static int COL_NAME		=0x20000000;
	private final static int COL_ALIAS		=0x10000000;
	private final static int COL_START_DATE	=0x08000000;
	private final static int COL_SPEC_ID	=0x04000000;
	private final static int COL_COMMENT	=0x02000000;
	
	private int _update_last_flags=0;
	private int _add_last_flags=0;
	private boolean update(String i_symbol,String i_coin_name,Item i_item,int i_flags) throws SdbException
	{
		try {
			
			//必要ならpreparedStatementを作り直す。
			if(this._update_last_flags!=i_flags){
				//PreparedStatementの再構築
				UpdateSqlBuilder isb=new UpdateSqlBuilder();
				isb.init(this._table_info.getTableName());
				if((i_flags&COL_ID)!=0){
					isb.add(DN_id);
				}
				if((i_flags&COL_SYMBOL)!=0){
					isb.add(DN_symbol);
				}
				if((i_flags&COL_NAME)!=0){
					isb.add(DN_name);
				}
				if((i_flags&COL_ALIAS)!=0){
					isb.add(DN_alias_id);
				}
				if((i_flags&COL_START_DATE)!=0){
					isb.add(DN_start_date);
				}
				if((i_flags&COL_SPEC_ID)!=0){
					isb.add(DN_spec_id);
				}
				if((i_flags&COL_COMMENT)!=0){
					isb.add(DN_comment);
				}
				this._ps_update=this._db.getConnection().prepareStatement(isb.finish(DN_symbol+"=? and "+DN_name+"=?"));
				this._update_last_flags=i_flags;
			}
			//PreperedStatementの実行
			int idx=0;
			if((i_flags&COL_ID)!=0){
				this._ps_update.setInt(++idx,i_item.id);
			}
			if((i_flags&COL_SYMBOL)!=0){
				this._ps_update.setString(++idx,i_item.coin_symbol);
			}
			if((i_flags&COL_NAME)!=0){
				this._ps_update.setString(++idx,i_item.coin_name);
			}
			if((i_flags&COL_ALIAS)!=0){
				PsUtils.setNullableInt(this._ps_update,++idx,i_item.alias_id);
			}
			if((i_flags&COL_START_DATE)!=0){
				PsUtils.setNullableLong(this._ps_update,++idx,i_item.start_date);
			}
			if((i_flags&COL_SPEC_ID)!=0){
				PsUtils.setNullableInt(this._ps_update,++idx,i_item.spec_id);
			}
			if((i_flags&COL_COMMENT)!=0){
				PsUtils.setNullableString(this._ps_update,++idx,i_item.comment);
			}
			this._ps_update.setString(++idx,i_item.coin_symbol);
			this._ps_update.setString(++idx,i_item.coin_name);
			this._ps_update.execute();
			boolean r=this._ps_update.getUpdateCount()>0;
			if(!r){
				return this.add(i_item, i_flags);
			}else{
				return true;
			}
		} catch (SQLException e){
			throw new SdbException(e);
		}		
	}
	/**
	 * 
	 * @param i_item
	 * @param i_flags
	 * @return
	 * @throws SdbException
	 */
	private boolean add(Item i_item,int i_flags) throws SdbException
	{
		try {
			//WHERE句NULLは許さん
			if(i_item.coin_symbol==null || i_item.coin_name==null){
				throw new SdbException();
			}			
			//必要ならpreparedStatementを作り直す。
			if(this._add_last_flags!=i_flags){
				//PreparedStatementの再構築
				InsertSqlBuilder isb=new InsertSqlBuilder();
				isb.init(this._table_info.getTableName());
				if((i_flags&COL_ID)!=0){
					isb.add(DN_id);
				}
				if((i_flags&COL_SYMBOL)!=0){
					isb.add(DN_symbol);
				}
				if((i_flags&COL_NAME)!=0){
					isb.add(DN_name);
				}
				if((i_flags&COL_ALIAS)!=0){
					isb.add(DN_alias_id);
				}
				if((i_flags&COL_START_DATE)!=0){
					isb.add(DN_start_date);
				}
				if((i_flags&COL_SPEC_ID)!=0){
					isb.add(DN_spec_id);
				}
				if((i_flags&COL_COMMENT)!=0){
					isb.add(DN_comment);
				}
				this._ps_insert=this._db.getConnection().prepareStatement(isb.finish());
				this._add_last_flags=i_flags;
			}
			//PreperedStatementの実行
			int idx=0;
			if((i_flags&COL_ID)!=0){
				this._ps_insert.setInt(++idx,i_item.id);
			}
			if((i_flags&COL_SYMBOL)!=0){
				this._ps_insert.setString(++idx,i_item.coin_symbol);
			}
			if((i_flags&COL_NAME)!=0){
				this._ps_insert.setString(++idx,i_item.coin_name);
			}
			if((i_flags&COL_ALIAS)!=0){
				PsUtils.setNullableInt(this._ps_insert,++idx,i_item.alias_id);
			}
			if((i_flags&COL_START_DATE)!=0){
				PsUtils.setNullableLong(this._ps_insert,++idx,i_item.start_date);
			}
			if((i_flags&COL_SPEC_ID)!=0){
				PsUtils.setNullableInt(this._ps_insert,++idx,i_item.spec_id);
			}			
			if((i_flags&COL_COMMENT)!=0){
				PsUtils.setNullableString(this._ps_insert,++idx,i_item.comment);
			}
			this._ps_insert.execute();
			boolean r=this._ps_insert.getUpdateCount()>0;
			return r;
		} catch (SQLException e){
			throw new SdbException(e);
		}
	}
	public static class Item
	{
		public Integer id;
		public String coin_symbol;
		public String coin_name;
		public Integer alias_id;
		public Long start_date;
		public Integer spec_id;
		public String comment;

		public Item(Integer i_id, String i_symbol, String i_coin_name,Integer i_alias_id,Long i_start_date,Integer i_spec_id, String i_comment)
		{
			this.id=i_id;
			this.coin_symbol=i_symbol;
			this.coin_name=i_coin_name;
			this.alias_id=i_alias_id;
			this.start_date=i_start_date;
			this.spec_id=i_spec_id;
			this.comment=i_comment;
			return;
		}
		public Item(ResultSet i_rs) throws SdbException
		{
			try{
				this.id=i_rs.getInt(DN_id);
				this.coin_symbol	=i_rs.getString(DN_symbol);
				this.coin_name		=i_rs.getString(DN_name);
				this.alias_id		=RsUtils.getNullableInt(i_rs,DN_alias_id);
				this.start_date		=RsUtils.getNullableLong(i_rs,DN_start_date);
				this.spec_id		=RsUtils.getNullableInt(i_rs,DN_spec_id);
				this.comment		=i_rs.getString(DN_comment);
			} catch (SQLException e){
				throw new SdbException(e);
			}
		}
	}

	/**
	 * シンボルに一致するidを返す。
	 * @param i_product_name
	 * @return
	 * 見つからなければNULL
	 * @throws SdbException
	 */
	public Item getItem(String i_symbol,String i_name) throws SdbException
	{
		try{
			ResultSet rs=null;
			try{
				//WHERE句NULLは許さん
				if(i_symbol==null || i_name==null){
					throw new SdbException();
				}
				this._ps_search_symbol.setString(1,i_symbol);
				this._ps_search_symbol.setString(2,i_name);
				rs=this._ps_search_symbol.executeQuery();
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
	public RowIterable<Item> getAllAsc() throws SdbException
	{
		try {
			ResultSet rs=this._ps_select_all_asc.executeQuery();
			return new RowIterable<Item>(rs,this._table_info);
		} catch (SQLException e){
			throw new SdbException(e);
		}
	}
	/**
	 * Alias_idを持つ行を選択して返す。
	 * @return
	 * @throws SdbException
	 */
	public RowIterable<Item> getAliasItems() throws SdbException
	{
		try {
			ResultSet rs=this._ps_select_all_alias.executeQuery();
			return new RowIterable<Item>(rs,this._table_info);
		} catch (SQLException e){
			throw new SdbException(e);
		}
	}
	/**
	 * idからアイテムを得る
	 * @param i_id
	 * @throws SdbException 
	 */
	public Item getItem(int i_id) throws SdbException
	{
		try {
			this._ps_select_item_id.setInt(1,i_id);
			ResultSet rs=this._ps_select_item_id.executeQuery();
			return rs.next()?new Item(rs):null;
		} catch (SQLException e){
			throw new SdbException(e);
		}		
	}
	public RowIterable<Item> getItemsBySpec(int i_spec_id) throws SdbException
	{
		try {
			this._ps_select_all_spec.setInt(1,i_spec_id);
			ResultSet rs=this._ps_select_all_spec.executeQuery();
			return new RowIterable<Item>(rs,this._table_info);
		} catch (SQLException e){
			throw new SdbException(e);
		}
	}
	public boolean isExistSpec(int i_spec_id) throws SdbException
	{
		try {
			this._ps_select_all_spec.setInt(1,i_spec_id);
			ResultSet rs=this._ps_select_all_spec.executeQuery();
			boolean ret=rs.next();
			rs.close();
			return ret;
		} catch (SQLException e){
			throw new SdbException(e);
		}
	}	
	/**
	 * Alias_idからルートIDのアイテムを得る。
	 * @throws SdbException 
	 */
	public Item getRootItemByAlias(int i_alias_id) throws SdbException
	{
		int aid=i_alias_id;
		for(;;){
			Item item=this.getItem(aid);
			if(item!=null){
				//alias_idがあるかチェック
				if(item.alias_id==null){
					//Alias_idがNULLの場合
					return item;
				}
				if((int)item.alias_id==(int)item.id){
					throw new SdbException();
				}
				aid=item.alias_id;
				//alias_idがある場合はさかのぼる
				continue;
			}
			//ID不整合(到達不能)
			return null;
		}
	}
	/**
	 * シンボル・名前から、Alias_idを持たないルートIDを返す。
	 * @param i_symbol
	 * @param i_name
	 * @return
	 * @throws SdbException
	 */
	public Item getRootItem(String i_symbol,String i_name) throws SdbException
	{
		Item it=this.getItem(i_symbol, i_name);
		if(it==null){
			return it;
		}
		if(it.alias_id!=null){
			return this.getRootItemByAlias(it.alias_id);
		}
		return it;
	}	
/*
	public RowIterable getAll() throws SdbException
	{
		try {
			ResultSet rs=this._ps_select_all.executeQuery();
			return new RowIterable(rs);
		} catch (SQLException e){
			throw new SdbException(e);
		}
	}
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
	}
*/

}