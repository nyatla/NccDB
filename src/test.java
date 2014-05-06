import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Date;

import jp.nyatla.nyansat.utils.SdbException;


public class test {

	
	public static void main(String[] args)
	{
		String[] t1={"-cmd","init"};
		String[] t2={
			"-cmd","coin_addlist",
			"-url","1,2,3",
			"-ua","Mozilla/5.0 (Windows NT 6.3; WOW64; rv:27.0) Gecko/20100101 Firefox/27.0",
			"-cookie","__cfduid=dfb33923dda01abf919d76a86135d949f1394332025012; itemMarking_forums_items=eJyrVjIxtzRUsjI0tjQxNjYyMDbQUTI3tjCBipgYGxqa1AJcMJLgB8k%2C; _ga=GA1.2.814650664.1394343048; cf_clearance=d7a067ee66c260422daf6af24422fdebc40c1aaa-1394362841-7200; session_id=4a6e4776f0fdcff2a6aab1af788a307d; modtids=%2C"};
		String[] t3={
				"-cmd","coin_exportcsv",
		};
		String[] t4={
				"-cmd","coin_importcsv",
		};
		String[] t5={
				"-cmd","url_exportcsv",
		};
		String[] t6={
				"-cmd","url_importcsv",
		};
		String[] t7={
				"-cmd","srv_importcsv",
		};
		String[] t8={
				"-cmd","srv_exportcsv","-service",
				"EXCHANGE@https://coinex.pw/,EXCHANGE@https://coinedup.com/,EXCHANGE@https://www.coins-e.com/,EXCHANGE@https://cryptorush.in/,EXCHANGE@https://www.cryptsy.com/"
		};
		String[] t9={
				"-cmd","cct_html_cache","-cct_db","d:\\NccDB_html_cache.sqlite3",
				"-ua","Mozilla/5.0 (Windows NT 6.3; WOW64; rv:27.0) Gecko/20100101 Firefox/27.0",
				"-cookie","__cfduid=dfb33923dda01abf919d76a86135d949f1394332025012; itemMarking_forums_items=eJxFk0lyBDEIBP_S5zmIHfw1h_9uYFpwzagqsen3caN4foBCXFwQgz9JGJagNMEhINhEL4HwKFwiPMSpXT7JoEFFYpJBDJL4uYTNJTVxiIaoQxNfAlGE18XuRWRdZJ0jOgShNbouOF0zn0s0qq84Nhp1syYxJAtqMvWocmqQXCcHTqo-D_GxIcBcXC58XWZOWNNAfF1GgDVD0Reo5Kg0JYpvjJywaiuJDtF6ypVgiMSXxBKqdekdT5IsNkku8hL3XimwX2IgpYGQIaeTYZPVxVtzhqhlExYxySrayfc0krBKv7U5xNAaXFwCpUGY3hUlX6_7HAJaLqTVfCtE8iVcXBWirOtAu3TqkehTRR2X5E0WsdV4fwKCJdZdkM4u6MBpMhrsc85DnwpRqOqhTUbG_nC3i1xcBJP8_QN64bWB; _ga=GA1.2.814650664.1394343048; session_id=698a4c8561fe71ade1d589a643201a6e; modtids=%2C; cf_clearance=232ef578a69046af53ac3f268534fd186128f6b1-1395888866-2700"
		};
		String[] t10={
				"-cmd","cct_import_urls",
				"-cct_db","d:\\NccDB_html_cache.sqlite3"
		};
		String[] t11={
				"-cmd","cct_scrape_url_from_list_thread",
				"-ua","Mozilla/5.0 (Windows NT 6.3; WOW64; rv:27.0) Gecko/20100101 Firefox/27.0",
				"-url","https://cryptocointalk.com/forum/1187-quark-based-cryptocoins/,https://cryptocointalk.com/forum/1181-new-cryptocoins/,https://cryptocointalk.com/forum/178-scrypt-cryptocoins/,https://cryptocointalk.com/forum/1184-scrypt-adaptive-n-cryptocoins/,https://cryptocointalk.com/forum/1186-scrypt-jane-cryptocoins/,https://cryptocointalk.com/forum/179-sha-256-cryptocoins/,https://cryptocointalk.com/forum/1194-sha-3-cryptocoins/,https://cryptocointalk.com/forum/1185-x11-cryptocoins/,https://cryptocointalk.com/forum/302-other-algo-cryptocoins/,https://cryptocointalk.com/forum/888-dying-other-algo-cryptocoins/,https://cryptocointalk.com/forum/1193-dying-x11-cryptocoins/,https://cryptocointalk.com/forum/1195-dying-sha-3-cryptocoins/,https://cryptocointalk.com/forum/1191-dying-sha-256-cryptocoins/,https://cryptocointalk.com/forum/1192-dying-scrypt-jane-cryptocoins/,https://cryptocointalk.com/forum/1190-dying-scrypt-adaptive-n-cryptocoins/,https://cryptocointalk.com/forum/1189-dying-scrypt-cryptocoins/,https://cryptocointalk.com/forum/1188-dying-quark-based-cryptocoins/",
				"-cookie","__cfduid=dfb33923dda01abf919d76a86135d949f1394332025012; itemMarking_forums_items=eJxF00l2xDAIBcC79LoXzEOulpe7RyAbtmX4EpL8-0Hz-Pwgp4k4p3w_yYYjhFfsFYvAlukypyxxGRHwFh2h2xWbg0BdkyMQvVZOlwZCy3SpRcn5soLUMjmqZiVOKyAtmyPKLZvDUVNI-gp7yyaT6vejqJsDcrpcXINfkXs-KJMspJWDOjWccSVXoOZCni42Omsl8ZwqC3LL1hBhy-Y8Zwi6wn2GOHumtCORMecDUcn4lpBmcA8Bj6CG1xAODlfA0C170FujKcFxthz6PA2NMKrgcPZHlIS1JUeoHl244Ap1jdAIKrXYK5h1XWdXMhI3OSYZLXv1nGRUP3NFwCvnAfRDXDDmEesLBI4VrElBtuvccoluF3vnqI0Qdo1tF0LvWeAVS-2fwGB-uHNkf_-DMrXt; _ga=GA1.2.814650664.1394343048; session_id=8466451b5e6217ee00ecfc1977b8a03a; modtids=%2C",
				"-cct_db","d:\\NccDB_html_cache.sqlite3"
		};
		String[] t12={
				"-cmd","cct_scrape_url_from_thread",
				"-ua","Mozilla/5.0 (Windows NT 6.3; WOW64; rv:27.0) Gecko/20100101 Firefox/27.0",
				"-cookie","__cfduid=dfb33923dda01abf919d76a86135d949f1394332025012; itemMarking_forums_items=eJxNk0l2xDAIBe_S6yxcMPEZcrW83D0COxLbMhST_PNJJH--eaVrgJW-Ppnhh5CtTVwwfYkHZW6idGM4vIjJiJFcIs6DcHl8eAKbGNElXh7jNUjHCAYps-mMKbPFqG7tCRvENnEetUyb-CBldkxSZrdBULN7xiBlDhrVUebgScocyEu0OowYU2h5ksakWp7ksUMtT8owK_e9xlxc7wVHzNoXXFw6zbKniJVcJ8sDdWXRM6ljlcdvhw7qreI1EyuotrHs8Ziqm-x-2DwOWakdw4cIP8T-iUVwk5NlLm1-X0sR7erLcYg8WXE9TNIxeQhF18qThWBqcrJgUWR_uYT79eJ4gH4_u_dLSJtcXI_2DvXOjhX9p6RfsvpPyWsW4PcPyeuzKg%2C%2C; _ga=GA1.2.814650664.1394343048; toggleCats=%2C232%2C; session_id=f4bda17e0b7840890b209ea4b2cc1df9; modtids=%2C",
				"-cct_db","d:\\NccDB_html_cache.sqlite3"
		};
		String[] t13={
				"-cmd","cct_import_coinlist",
				"-cct_db","d:\\html_cache.db"
		};
		String[] t14={
				"-cmd","imageurlset",
				"-imgsrc","D:\\db\\nccdb\\img",
				"-imgdst","D:\\public_html\\ccsatoshi\\web\\cimg"
		};		
		String[] test=t14;
		Main.main(test);
/*		
		try {
			System.out.println(CctHtmlCache.encodePath("https://cryptocointalk.com/topic/9802-04-23-★★-annfeel-feelscoin-rare-scrypt-hybrid-pow-pos-escrow-ipo-that-feel/"));
		} catch (SdbException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
	/*	
		String[] s={
			"Block Crawler",
			"http://bter.com/tradeaaa"
		};
		for(int i=0;i<s.length;i++){
			System.out.println(s[i].matches("Block(chain)? ((Crawler)|(Explorers?)).*"));
		}*/
	}

	/**
	 * @param args
	 */
/*	public static void main(String[] args) {
		// TODO Auto-generated method stub
		System.out.print((new Date(1392526205*1000L)).toString());
	}
*/
}
