import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import jp.nyatla.nccdb.table.CoinMasterTable;
import jp.nyatla.nyansat.db.basic.RowIterable;
import jp.nyatla.nyansat.db.basic.SqliteDB;
import jp.nyatla.nyansat.utils.SdbException;


public class ImageListImport
{
	private static File getImageFile(String i_dir,String i_symbol,String i_name)
	{
		String ext[]={"jpg","jpeg","png","gif"};
		String prefix[]={
			i_dir+"\\"+i_symbol+"."+i_name,
			i_dir+"\\_"+i_symbol+"."+i_name
		};
		for(int i=0;i<prefix.length;i++){
			for(int i2=0;i2<ext.length;i2++){
				String n=prefix[i]+"."+ext[i2];
				File f=new File(n);
				if(f.exists()){
					return f;
				}
			}
		}
		return null;
		
	}
	/**
	 * CoinMasterのコインシンボル名をもとに画像ファイルを検索して、存在すればマッピングテーブルと出力ディレクトリへ画像を出力する。
	 * @param ap
	 * @throws SdbException
	 */
	public static void updateImageUrl(NccDBAppArgHelper ap) throws SdbException
	{
		SqliteDB db=ap.getNccDB();
		String dir_src=ap.getString("-imgsrc",".\\imgsrc");
		String dir_dest=ap.getString("-imgdst",".\\imgdst");
		Logger.log("Starting image search.");
		CoinMasterTable ctt=null;
		CoinMasterTable.Item err=null;
		ArrayList<String> flist=new ArrayList<String>();
		try{
			ctt=new CoinMasterTable(db);
			RowIterable<CoinMasterTable.Item> lctt=ctt.getAll();
			for(CoinMasterTable.Item it : lctt){
				err=it;
				if(it.alias_id!=null){
					Logger.log("[ALIAS]"+it.coin_symbol+" "+it.coin_name);
					continue;
				}
				//ファイル取得
				File src=getImageFile(dir_src,it.coin_symbol,it.coin_name);
				if(src==null){
					Logger.log("[NOFILE]"+it.coin_symbol+" "+it.coin_name);
					continue;
				}
				flist.add(src.getName());
				//失敗したら
				BufferedImage img = ImageIO.read(src);
				int sh=img.getHeight();
				int sw=img.getWidth();
				if(sw>sh){
					if(sw>256){
						sh=sh*256/sw;
						sw=256;
					}
				}else{
					if(sh>256){
						sw=sw*256/sh;
						sh=256;
					}
				}
		        Image I03 = img.getScaledInstance(sw,sh, Image.SCALE_SMOOTH);
			
				
				
				//書き出し
				BufferedImage resizeImage = new BufferedImage(sw,sh,BufferedImage.TYPE_4BYTE_ABGR);
				resizeImage.getGraphics().drawImage(I03,0,0,sw,sh,null);
				File df=new File(dir_dest+"\\"+it.id+".png");
//				File df=new File(dir_dest+"\\_"+it.coin_symbol+"."+it.coin_name+".png");
				ImageIO.write(resizeImage,"png",df);
				Logger.log("[ADD]"+it.coin_symbol+" "+it.coin_name+" id="+it.id);
			}
		} catch (IOException e) {
			System.err.println("[IOERROR]"+err.coin_symbol+":"+err.coin_name);
			throw new SdbException(e);
		}finally{
			if(ctt!=null){
				ctt.dispose();
			}
		}
		//参照されなかった元ファイルの一覧を作る。
		Logger.log("No file coins");
		File sdir = new File(dir_src);
		File[] files = sdir.listFiles();
		for(int i=0;i<files.length;i++){
			for(int i2=0;i2<flist.size();i2++){
				if(flist.get(i2).equals(files[i].getName())){
					flist.remove(i2);
					break;
				}
			}
		}
		for(String s:flist){
			Logger.log(s);
		}
		Logger.log("done.");
	}	
	public static boolean run(String i_cmd,NccDBAppArgHelper args) throws SdbException
	{
		if(i_cmd.compareTo("imageurlset")==0){
			updateImageUrl(args);
		}else{
			return false;
		}
		return true;
	}
}
