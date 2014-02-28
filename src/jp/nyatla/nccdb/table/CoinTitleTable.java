package jp.nyatla.nccdb.table;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;

import jp.nyatla.nyansat.db.basic.table.BaseTable;
import jp.nyatla.nyansat.db.basic.table.SqliteDB;
import jp.nyatla.nyansat.utils.CsvReader;
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
 * <li>id_coin_algorism int - アルゴリズムタイプ
 * </ul>
 * unique(cpu_id,processor_number)
 * </p>
 */
public class CoinTitleTable extends BaseTable
{
	public final static String NAME="coin_title";
	public final static String DN_id="id";
	public final static String DN_symbol="symbol";
	public final static String DN_name="name";
	

	
	protected String createTableDefinisitonStr()
	{
		return "("+
			DN_id+" integer,"+
			DN_symbol+" text,"+
			DN_name+" text,"+
			"unique("+DN_symbol+","+DN_name+"),"+
			"primary key("+DN_id+"))";
	}
	@Override
	public void dispose() throws SdbException
	{
		try {
			this._ps_search_symbol.close();
			this._ps_insert.close();
			this._ps_delete.close();
			this._ps_update.close();
		} catch (SQLException e) {
			throw new SdbException(e);
		}
	}

	private PreparedStatement _ps_insert;
	private PreparedStatement _ps_delete;
	private PreparedStatement _ps_search_symbol;
	private PreparedStatement _ps_update;
	public CoinTitleTable(SqliteDB i_db) throws SdbException
	{
		this(i_db,NAME);
	}
	public CoinTitleTable(SqliteDB i_db,String i_table_name) throws SdbException
	{
		super(i_db,i_table_name);
		try {
			this._ps_update=this._db.getConnection().prepareStatement("update "+this._tbl_name+" set "
				+DN_symbol+"=?,"
				+DN_name+"=? where "+DN_id+"=?;");
			this._ps_insert=this._db.getConnection().prepareStatement("insert or ignore into "+this._tbl_name+
				"("+DN_symbol+","+DN_name+") values(?,?);");
			this._ps_search_symbol=this._db.getConnection().prepareStatement("select * from "+this._tbl_name +
				" where "+DN_symbol+"=? and "+DN_name+"=?;");
			this._ps_delete=this._db.getConnection().prepareStatement("delete from "+this._tbl_name+" where "+DN_id+"=?;");
		} catch (SQLException e) {
			throw new SdbException(e);
		}
	}
	/**
	 * 入力データを優先してデータを更新する。
	 * @param i_symbol
	 * @param i_coin_name
	 * @return
	 * @throws SdbException
	 */
	public boolean update(String i_symbol,String i_coin_name) throws SdbException
	{
		Item item=this.getItem(i_symbol,i_coin_name);
		if(item!=null){
			//存在するなら何もしない。
			try {
				this._ps_update.setString(1,item.coin_symbol);
				this._ps_update.setString(2,item.coin_name);
				this._ps_update.setInt(3,item.id);
				return this._ps_update.executeUpdate()>0;
			} catch (SQLException e){
				throw new SdbException(e);
			}
		}else{
			//存在しないなら追加
			return this.add(i_symbol, i_coin_name);
		}
	}
	public boolean add(String i_symbol,String i_coin_name) throws SdbException
	{
		try {
			this._ps_insert.setString(1,i_symbol);
			this._ps_insert.setString(2,i_coin_name);
			this._ps_insert.execute();
			boolean r=this._ps_insert.getUpdateCount()>0;
			return r;
		} catch (SQLException e){
			throw new SdbException(e);
		}
	}
/*
	public void importCSV(String i_file_path) throws SdbException
	{
		try {
			//ファイル読み込み
			CsvReader csv=new CsvReader(i_file_path);

			//列インデックスを得る。
			int coin_symbol_idx=csv.getIndex(DN_symbol);
			int coin_name_idx=csv.getIndex(DN_name);
			String[] l=csv.readNext();
			while(l!=null){
				//1行づつパースして追記
				boolean r=this.add(
						l[coin_symbol_idx],
						l[coin_name_idx]);
				this.info(this,"["+(r?"ADD":"DROP")+"]"+l[coin_symbol_idx]+" "+l[coin_name_idx]);
				l=csv.readNext();
			}
			return;
		} catch (Throwable e){
			throw new SdbException(e);
		}
	}
*/
	public static class Item
	{
		public int id;
		public String coin_symbol;
		public String coin_name;
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
			PreparedStatement s=null;
			ResultSet rs=null;
			Item result=new Item();
			try{
				this._ps_search_symbol.setString(1,i_symbol);
				this._ps_search_symbol.setString(2,i_name);
				rs=this._ps_search_symbol.executeQuery();
				if(rs.next()){
					result.id=rs.getInt(DN_id);
					result.coin_symbol=rs.getString(DN_symbol);
					result.coin_name=rs.getString(DN_name);
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
}