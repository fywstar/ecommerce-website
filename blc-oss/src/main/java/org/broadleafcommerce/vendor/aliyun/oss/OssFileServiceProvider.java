package org.broadleafcommerce.vendor.aliyun.oss;

import com.aliyun.oss.OSSClient;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.PutObjectRequest;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadleafcommerce.common.file.FileServiceException;
import org.broadleafcommerce.common.file.domain.FileWorkArea;
import org.broadleafcommerce.common.file.service.BroadleafFileService;
import org.broadleafcommerce.common.file.service.FileServiceProvider;
import org.broadleafcommerce.common.file.service.type.FileApplicationType;
import org.broadleafcommerce.common.site.domain.Site;
import org.broadleafcommerce.common.web.BroadleafRequestContext;

import javax.annotation.Resource;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by billyang on 2017/2/8.
 */
//@Service("blOssFileServiceProvider")
public class OssFileServiceProvider implements FileServiceProvider {
    private static final Log LOG = LogFactory.getLog(OssFileServiceProvider.class);

    @Resource(name = "blFileService")
    protected BroadleafFileService blFileService;

    private String ossaccessId = "HmTjtVwGWaEvbDw5";
    private String ossaccessKey = "GEKUNH2wv7fen6LxQhH2655ZcgRvmd";


    private String ossendPoint = "https://oss-cn-shenzhen.aliyuncs.com";
    private String ossbucket = "immotor-china";

    @Override
    public File getResource(String s) {
        return getResource(s, FileApplicationType.ALL);
    }

    @Override
    public File getResource(String s, FileApplicationType fileApplicationType) {
        String objKey = buildResourceName(s);
        File returnFile = blFileService.getLocalResource(objKey);
        OutputStream outputStream = null;

        OSSClient client = getOSSClient();
        OSSObject object = client.getObject(ossbucket, objKey);
        InputStream inputStream = object.getObjectContent();
        try {
            if (!returnFile.getParentFile().exists()) {
                if (!returnFile.getParentFile().mkdirs()) {
                    if (!returnFile.getParentFile().exists()) {
                        throw new RuntimeException("Unable to create parent directories for file: " + s);
                    }
                }
            }
            outputStream = new FileOutputStream(returnFile);
            int read = 0;
            byte[] bytes = new byte[1024];

            while ((read = inputStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, read);
            }
        } catch (IOException ioe) {
            throw new RuntimeException("Error writing aliyun oss file to local file system", ioe);
        } finally {
            client.shutdown();

            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    throw new RuntimeException("Error closing input stream while writing aliyun oss file to file system", e);
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    throw new RuntimeException("Error closing output stream while writing aliyun oss file to file system", e);
                }
            }

        }

        return returnFile;
    }

    @Override
    public void addOrUpdateResources(FileWorkArea fileWorkArea, List<File> list, boolean b) {
        addOrUpdateResourcesForPaths(fileWorkArea, list, b);
    }

    @Override
    public List<String> addOrUpdateResourcesForPaths(FileWorkArea fileWorkArea, List<File> list, boolean b) {
        List<String> resourcePaths = new ArrayList<String>();
        OSSClient client = getOSSClient();
        for (File srcFile : list) {
            if (!srcFile.getAbsolutePath().startsWith(fileWorkArea.getFilePathLocation())) {
                throw new FileServiceException("Attempt to update file " + srcFile.getAbsolutePath() +
                        " that is not in the passed in WorkArea " + fileWorkArea.getFilePathLocation());
            }
            String fileName = srcFile.getAbsolutePath().substring(fileWorkArea.getFilePathLocation().length());
            String url = FilenameUtils.separatorsToUnix(fileName);
            String resourceName = buildResourceName(url);
            client.putObject(new PutObjectRequest(ossbucket, resourceName, srcFile));
            resourcePaths.add(fileName);
        }
        return resourcePaths;
    }

    @Override
    public boolean removeResource(String s) {
        OSSClient client = getOSSClient();
        client.deleteObject(ossbucket, buildResourceName(s));
        File returnFile = blFileService.getLocalResource(buildResourceName(s));
        if (returnFile != null) {
            returnFile.delete();
        }
        client.shutdown();
        return true;
    }

    private OSSClient getOSSClient() {
        OSSClient client = new OSSClient(ossendPoint, ossaccessId, ossaccessKey);

        return client;
    }

    /**
     * hook for overriding name used for resource in aliyun oss
     *
     * @param
     * @return
     */
    protected String buildResourceName(String name) {
        if (name.startsWith("/")) {
            name = name.substring(1);
        }
        String siteSpecificResourceName = getSiteSpecificResourceName(name);
        return siteSpecificResourceName;
    }

    protected String getSiteSpecificResourceName(String resourceName) {
        BroadleafRequestContext brc = BroadleafRequestContext.getBroadleafRequestContext();
        if (brc != null) {
            Site site = brc.getNonPersistentSite();
            if (site != null) {
                String siteDirectory = getSiteDirectory(site);
                if (resourceName.startsWith("/")) {
                    resourceName = resourceName.substring(1);
                }
                return FilenameUtils.concat(siteDirectory, resourceName);
            }
        }

        return resourceName;
    }

    protected String getSiteDirectory(Site site) {
        String siteDirectory = "site-" + site.getId();
        return siteDirectory;
    }

}
