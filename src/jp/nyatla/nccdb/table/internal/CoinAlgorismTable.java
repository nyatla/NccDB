package jp.nyatla.nccdb.table.internal;

import jp.nyatla.nyansat.db.basic.table.internal.InternalNameTable;


public final class CoinAlgorismTable extends InternalNameTable
{
	public final static int UNKNOWN	=0;
	public final static int SHA256D	=1;
	public final static int SCRYPT	=2;
	public final static int QUARK	=3;
	private final static String[] _table={
		"UNKNOWN",
		"SHA256D",
		"SCRYPT",
		"QUARK"		
	};
	private final static CoinAlgorismTable _single=new CoinAlgorismTable();
	public static CoinAlgorismTable getSingleton()
	{
		return _single;
	}
	private CoinAlgorismTable()
	{
		super(_table);
	}	
}
