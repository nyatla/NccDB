package jp.nyatla.nccdb.table.internal;

import jp.nyatla.nyansat.db.basic.table.internal.InternalNameTable;
import jp.nyatla.nyansat.utils.SdbException;

public final class ServiceTypeTable extends InternalNameTable
{
	public final static int UNKNOWN			=0;
	public final static int WEBSITE			=1;
	public final static int BLOCKEXPROLER	=2;
	public final static int POOL			=3;
	public final static int EXCHANGE		=4;
	private final static String[] _table={
		"UNKNOWN",
		"WEBSITE",
		"BLOCKEXPROLER",
		"POOL",
		"EXCHANGE"
	};
	private final static ServiceTypeTable _single=new ServiceTypeTable();
	public static ServiceTypeTable getSingleton()
	{
		return _single;
	}
	private ServiceTypeTable()
	{
		super(_table);
	}	
}
