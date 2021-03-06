package com.ft.methodecontentcollectionmapper.mapping;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ft.methodecontentcollectionmapper.client.DocumentStoreApiClient;
import com.ft.methodecontentcollectionmapper.exception.ContentCollectionMapperException;
import com.ft.methodecontentcollectionmapper.exception.TransformationException;
import com.ft.methodecontentcollectionmapper.exception.UnsupportedTypeException;
import com.ft.methodecontentcollectionmapper.exception.UuidResolverException;
import com.ft.methodecontentcollectionmapper.model.ContentCollection;
import com.ft.methodecontentcollectionmapper.model.EomFile;
import com.ft.methodecontentcollectionmapper.model.EomLinkedObject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class EomFileToContentCollectionMapperTest {

  private static final String CONTENT_COLLECTION_UUID = "5519a61c-c684-11e6-9043-7e34c07b46ef";
  private static final String ITEM_UUID_1 = "5519a61c-c684-11e6-9043-7e34c07b46ef";
  private static final String ITEM_UUID_2 = "5519a61c-c684-11e6-9043-7e34c07b46ef";
  private static final String CPH_UUID = "38b81198-f18e-11e8-911c-a20996806a68";
  private static final String CPH_ORIGINAL_UUID = "36f9b014-16ee-319d-b208-28b33e6b8d5f";

  private static final String EOM_WEB_CONTAINER = "EOM::WebContainer";
  private static final String STORY_PACKAGE_TYPE = "editorsChoice";
  private static final String CONTENT_PACKAGE_TYPE = "content-package";
  private static final String TRANSACTION_ID = "tid_ab!430d8ef";
  private static final Date LAST_MODIFIED = new Date();

  private static final String ATTRIBUTES_TEMPLATE =
      "<!DOCTYPE ObjectMetadata SYSTEM \"/SysConfig/Classify/FTDWC2/classify.dtd\">\n"
          + "<ObjectMetadata><FTcom><DIFTcomWebType>%s</DIFTcomWebType>\n"
          + "<autoFill/>\n<footwellDedupe/>\n<displayCode/>\n<searchAge/>\n<agingRule/>\n"
          + "<markDeleted>False</markDeleted>\n</FTcom>\n<OutputChannels><DIFTcom><DIFTcomWebID/>\n"
          + "</DIFTcom>\n</OutputChannels>\n</ObjectMetadata>";
  private static final String SYSTEM_ATTRIBUTES =
      "<props><productInfo><name>FTcom</name>\n<issueDate>20160709</issueDate>\n</productInfo>\n"
          + "<summary/>\n<archiveUuid>244424ae-fc23-11df-a389-00144feabdc0</archiveUuid>\n<workFolder>/FT/WorldNews</workFolder>\n"
          + "<subFolder>Asia</subFolder>\n<templateName>/FT/Library/Masters/DwcTemplates/Story ContentCollection.dwc</templateName>\n</props>";

  private EomFile eomFileContentCollection;
  private EomFileToContentCollectionMapper eomContentCollectionMapper;
  private BlogUuidResolver blogUuidResolver;
  private DocumentStoreApiClient docStoreClient;

  @Before
  public void setUp() {
    blogUuidResolver = mock(BlogUuidResolver.class);
    docStoreClient = mock(DocumentStoreApiClient.class);
    eomContentCollectionMapper =
        new EomFileToContentCollectionMapper(docStoreClient, blogUuidResolver);
  }

  @Test(expected = UnsupportedTypeException.class)
  public void shouldThrowUnsupportedTypeExceptionForNotContentCollectionTypes() {
    mockContentCollection(CONTENT_COLLECTION_UUID, "");
    eomContentCollectionMapper.mapPackage(eomFileContentCollection, TRANSACTION_ID, LAST_MODIFIED);
  }

  @Test
  public void shouldReturnStoryPackageWithNoItems() {
    mockContentCollection(CONTENT_COLLECTION_UUID, STORY_PACKAGE_TYPE);
    final ContentCollection actualStoryPackage =
        eomContentCollectionMapper.mapPackage(
            eomFileContentCollection, TRANSACTION_ID, LAST_MODIFIED);

    assertThat(actualStoryPackage.getItems().size(), equalTo(0));
  }

  @Test
  public void shouldReturnContentPackageWithNoItems() {
    mockContentCollection(CONTENT_COLLECTION_UUID, CONTENT_PACKAGE_TYPE);
    final ContentCollection actualContentPackage =
        eomContentCollectionMapper.mapPackage(
            eomFileContentCollection, TRANSACTION_ID, LAST_MODIFIED);

    assertThat(actualContentPackage.getItems().size(), equalTo(0));
  }

  @Test
  public void shouldNotThrowExceptionIfItemListIsNull() throws Exception {
    mockContentCollection(CONTENT_COLLECTION_UUID, STORY_PACKAGE_TYPE, (EomLinkedObject[]) null);
    final ContentCollection actualStoryPackage =
        eomContentCollectionMapper.mapPackage(
            eomFileContentCollection, TRANSACTION_ID, LAST_MODIFIED);

    assertThat(actualStoryPackage, is(notNullValue()));
    assertThat(actualStoryPackage.getItems(), is(notNullValue()));
    assertThat(actualStoryPackage.getItems().size(), is(0));
  }

  @Test
  public void shouldReturnAllItemsStoryPackage() {
    mockContentCollection(
        CONTENT_COLLECTION_UUID,
        STORY_PACKAGE_TYPE,
        new EomLinkedObject.Builder().withUuid(ITEM_UUID_1).withType("Story").build(),
        new EomLinkedObject.Builder().withUuid(ITEM_UUID_2).withType("Story").build());
    final ContentCollection actualStoryPackage =
        eomContentCollectionMapper.mapPackage(
            eomFileContentCollection, TRANSACTION_ID, LAST_MODIFIED);

    assertThat(actualStoryPackage.getItems().size(), equalTo(2));
  }

  @Test
  public void shouldReturnAllItemsContentPackage() {
    mockContentCollection(
        CONTENT_COLLECTION_UUID,
        CONTENT_PACKAGE_TYPE,
        new EomLinkedObject.Builder().withUuid(ITEM_UUID_1).withType("Story").build(),
        new EomLinkedObject.Builder().withUuid(ITEM_UUID_2).withType("Story").build());
    final ContentCollection actualStoryPackage =
        eomContentCollectionMapper.mapPackage(
            eomFileContentCollection, TRANSACTION_ID, LAST_MODIFIED);

    assertThat(actualStoryPackage.getItems().size(), equalTo(2));
  }

  @Test
  public void shouldResolveBlogWhenOriginalUuidMissing() throws IOException {
    mockContentCollection(
        CONTENT_COLLECTION_UUID,
        CONTENT_PACKAGE_TYPE,
        new EomLinkedObject.Builder().withUuid(ITEM_UUID_1).withType("Story").build(),
        new EomLinkedObject.Builder()
            .withUuid("2e94f688-9dcd-11e7-b832-26b3e1fb1780")
            .withType("Story")
            .withAttributes(
                new String(Files.readAllBytes(Paths.get("src/test/resources/blog-attributes.xml"))))
            .build());
    when(blogUuidResolver.resolveUuid(
            "http://ftalphaville.ft.com/?p=2193913", "2193913", TRANSACTION_ID))
        .thenReturn("98f3d2e8-f392-4401-b817-040b36b51e9d");
    final ContentCollection actualStoryPackage =
        eomContentCollectionMapper.mapPackage(
            eomFileContentCollection, TRANSACTION_ID, LAST_MODIFIED);

    assertThat(actualStoryPackage.getItems().size(), equalTo(2));
    assertEquals(
        actualStoryPackage.getItems().get(1).getUuid(), "98f3d2e8-f392-4401-b817-040b36b51e9d");
  }

  @Test(expected = UuidResolverException.class)
  public void shouldThrowIfCantResolveBlogWhenOriginalUuidMissing() throws IOException {
    mockContentCollection(
        CONTENT_COLLECTION_UUID,
        CONTENT_PACKAGE_TYPE,
        new EomLinkedObject.Builder().withUuid(ITEM_UUID_1).withType("Story").build(),
        new EomLinkedObject.Builder()
            .withUuid("2e94f688-9dcd-11e7-b832-26b3e1fb1780")
            .withType("Story")
            .withAttributes(
                new String(Files.readAllBytes(Paths.get("src/test/resources/blog-attributes.xml"))))
            .build());
    when(blogUuidResolver.resolveUuid(
            "http://ftalphaville.ft.com/?p=2193913", "2193913", TRANSACTION_ID))
        .thenThrow(new UuidResolverException("can't resolve"));
    eomContentCollectionMapper.mapPackage(eomFileContentCollection, TRANSACTION_ID, LAST_MODIFIED);
  }

  @Test
  public void shouldReturnLinkedUuidWhenNotBlogAndOriginalUuidMissing() throws IOException {
    mockContentCollection(
        CONTENT_COLLECTION_UUID,
        CONTENT_PACKAGE_TYPE,
        new EomLinkedObject.Builder()
            .withUuid(CPH_UUID)
            .withType("Story")
            .withAttributes(
                new String(
                    Files.readAllBytes(
                        Paths.get("src/test/resources/invalid-category-blog-attributes.xml"))))
            .build());
    ContentCollection actualStoryPackage =
        eomContentCollectionMapper.mapPackage(
            eomFileContentCollection, TRANSACTION_ID, LAST_MODIFIED);

    assertThat(actualStoryPackage.getItems().size(), equalTo(1));
    assertEquals(actualStoryPackage.getItems().get(0).getUuid(), CPH_UUID);
  }

  @Test
  public void shouldResolveContentPlaceholderWithOriginalUuid() throws IOException {
    mockContentCollection(
        CONTENT_COLLECTION_UUID,
        CONTENT_PACKAGE_TYPE,
        new EomLinkedObject.Builder()
            .withUuid(CPH_UUID)
            .withType("Story")
            .withAttributes(
                new String(
                    Files.readAllBytes(
                        Paths.get("src/test/resources/content-placeholder-attributes.xml"))))
            .build());
    when(docStoreClient.canResolveUUID(CPH_ORIGINAL_UUID, TRANSACTION_ID)).thenReturn(true);
    ContentCollection actualContentCollection =
        eomContentCollectionMapper.mapPackage(
            eomFileContentCollection, TRANSACTION_ID, LAST_MODIFIED);

    assertThat(actualContentCollection.getItems().size(), equalTo(1));
    assertEquals(CPH_ORIGINAL_UUID, actualContentCollection.getItems().get(0).getUuid());
  }

  @Test(expected = ContentCollectionMapperException.class)
  public void shouldTrowContentPlaceholderWhenOriginalUuidCannotBeResolved() throws IOException {
    mockContentCollection(
        CONTENT_COLLECTION_UUID,
        CONTENT_PACKAGE_TYPE,
        new EomLinkedObject.Builder()
            .withUuid(CPH_UUID)
            .withType("Story")
            .withAttributes(
                new String(
                    Files.readAllBytes(
                        Paths.get("src/test/resources/content-placeholder-attributes.xml"))))
            .build());
    when(docStoreClient.canResolveUUID(CPH_ORIGINAL_UUID, TRANSACTION_ID))
        .thenThrow(new IllegalArgumentException());
    eomContentCollectionMapper.mapPackage(eomFileContentCollection, TRANSACTION_ID, LAST_MODIFIED);
  }

  @Test(expected = TransformationException.class)
  public void shouldThrowIfCantParseAttributes() throws IOException {
    mockContentCollection(
        CONTENT_COLLECTION_UUID,
        CONTENT_PACKAGE_TYPE,
        new EomLinkedObject.Builder().withUuid(ITEM_UUID_1).withType("Story").build(),
        new EomLinkedObject.Builder()
            .withUuid("2e94f688-9dcd-11e7-b832-26b3e1fb1780")
            .withType("Story")
            .withAttributes("<incorrect><xml")
            .build());
    eomContentCollectionMapper.mapPackage(eomFileContentCollection, TRANSACTION_ID, LAST_MODIFIED);
  }

  private void mockContentCollection(
      final String uuid, final String webType, final EomLinkedObject... contentCollectionItems) {
    eomFileContentCollection =
        new EomFile.Builder()
            .withUuid(uuid)
            .withType(EOM_WEB_CONTAINER)
            .withAttributes(String.format(ATTRIBUTES_TEMPLATE, webType))
            .withWorkflowStatus("")
            .withSystemAttributes(SYSTEM_ATTRIBUTES)
            .withUsageTickets(null)
            .withLinkedObjects(contentCollectionItems)
            .build();
  }
}
