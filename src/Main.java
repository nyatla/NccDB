


import jp.nyatla.nyansat.utils.SdbException;


public class Main
{
	public static final String VERSION="1.4.5";	
	public static void main(String[] args)
	{
		System.setProperty("file.encoding","UTF-8");
		try{
			NccDBAppArgHelper a=new NccDBAppArgHelper(args);			
			String c=a.getString("-cmd",null);
			if(c==null){
				throw new SdbException("-cmd not found");
			}
			if(CoinListCsvIo.run(c,a)){
			}else if(UrlListCsvIo.run(c,a)){
			}else if(ServiceCoinCsvIo.run(c,a)){
			}else if(CctHtmlCache.run(c, a)){
			}else if(ImageListImport.run(c, a)){
			}else{
				throw new Exception("Bad -cmd");
			}
		}catch(Throwable e){
			System.out.println("nccdb - NyatlaCryptCoinDatabase version "+VERSION);
			System.out.println(CoinListCsvIo.readme());
			System.out.println(UrlListCsvIo.readme());
			System.out.println(ServiceCoinCsvIo.readme());
			System.out.println(CctHtmlCache.readme());
			e.printStackTrace();
		}
	}
}
