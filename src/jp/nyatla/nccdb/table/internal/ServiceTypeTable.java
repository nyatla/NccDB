package jp.nyatla.nccdb.table.internal;

import jp.nyatla.nyansat.db.basic.table.internal.InternalNameTable;

public final class ServiceTypeTable extends InternalNameTable
{
	public final static int UNKNOWN			=0;
	public final static int WEBSITE			=1;
	public final static int BLOCKEXPROLER	=2;
	public final static int POOL			=3;
	public final static int EXCHANGE		=4;
	public final static int SNS				=5;
	public final static int FAUCETS			=6;
	public final static int FORUM			=7;
	public final static int GAME			=8;
	public final static int CLIENT			=9;
	public final static int SOURCECODE		=10;
	public final static int MINER			=11;
	private final static String[] _table={
		"UNKNOWN",
		"WEBSITE",
		"BLOCKEXPROLER",
		"POOL",
		"EXCHANGE",
		"SNS",
		"FAUCET",
		"FORUM",
		"GAME",
		"CLIENT",
		"SOURCECODE",
		"MINER"
		
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
