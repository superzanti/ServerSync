package runme;

import java.io.File;

import com.superzanti.serversync.util.PathUtils;

public class Delete implements Runnable {

	public static boolean finished = false;

	@Override
	public void run() {
		try {
			File[] filesToDelete = new File[] {};
			File deleteDir = new File("serversync_delete/");
			deleteDir.mkdirs();

			File workDir = new File("");
			File minecraftDir = PathUtils.getMinecraftDirectory(workDir);
			
			System.out.println(minecraftDir.getAbsolutePath());

			if (deleteDir.exists() && deleteDir.isDirectory()) {
				filesToDelete = deleteDir.listFiles();
			}

			if (filesToDelete != null && filesToDelete.length > 0) {

				for (File file : filesToDelete) {
					String converted = file.getName().replace("_$_", "/");
					File deleteMe = new File(minecraftDir + converted);
					long timeout = System.currentTimeMillis();
					while (!deleteMe.delete()) { // Run until system releases resources or timeout in 10 seconds
						if (System.currentTimeMillis() - timeout > 10000) {
							break;
						}
						System.out.println("Sleeping for 2ms");
						Thread.sleep(200);
					}
					String path = deleteMe.getAbsolutePath();
					System.out.println("Successfully deleted " + path);
					file.delete();
				}

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		finished = true;
		System.out.println("Finished running delete code");
	}

}
