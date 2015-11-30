package runme;

import java.io.File;

public class Delete implements Runnable {
	
	public static boolean finished;

	@Override
	public void run() {
		try {
			System.out.println("Sleeping for 3 seconds");
			//Thread.sleep(1000);
			
			File[] filesToDelete = new File[] {};
			File deleteDir = new File("serversync_delete/");
			deleteDir.mkdirs();
			
			File minecraftDir = new File("");
			String useable = minecraftDir.getAbsolutePath().replace("\\", "/");
			String[] brokenOut = useable.split("/");
			int len = brokenOut.length - 1;
			String newUrl = "";
			for (int i = 0; i < len; i++) {
				newUrl += brokenOut[i]+"/";
			}
			minecraftDir = new File(newUrl);

			System.out.println(deleteDir.getPath());

			if (deleteDir.exists() && deleteDir.isDirectory()) {
				filesToDelete = deleteDir.listFiles();
			}

			if (filesToDelete != null && filesToDelete.length > 0) {

				for (File file : filesToDelete) {
					String converted = file.getName().replace("_$_", "/");
					File deleteMe = new File(minecraftDir + converted);
					if (deleteMe.delete()) {
						String path = deleteMe.getAbsolutePath();
						System.out.println("Successfully deleted " + path);
						file.delete();
					}
				}

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("Finished running delete code");
	}

}
