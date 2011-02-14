package org.sakaiproject.nakamura.persondirectory.providers;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sakaiproject.nakamura.api.lite.content.Content;
import org.sakaiproject.nakamura.api.persondirectory.PersonProvider;
import org.sakaiproject.nakamura.api.persondirectory.PersonProviderException;
import org.sakaiproject.nakamura.api.profile.ProviderSettings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@RunWith(MockitoJUnitRunner.class)
public class PersonProfileProviderAdapterTest {

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  ProviderSettings ps1;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  PersonProvider personProvider;

  @SuppressWarnings("unchecked")
  @Test
  public void testGetProvidedMap() throws InterruptedException, ExecutionException,
      PersonProviderException {
    PersonProfileProviderAdapter pppa = new PersonProfileProviderAdapter();
    pppa.personProvider = this.personProvider;
    ArrayList<ProviderSettings> list = new ArrayList<ProviderSettings>();
    list.add(ps1);

    Map<String, Object> profileSection = new HashMap<String, Object>();
    profileSection.put("foo", "bar");

    Content content = new Content(null, null);
    when(ps1.getNode()).thenReturn(content);
    when(personProvider.getProfileSection(content)).thenReturn(profileSection);

    Map<Content, Future<Map<String, Object>>> result = (Map<Content, Future<Map<String, Object>>>) pppa.getProvidedMap(list);
    Future<Map<String, Object>> fut = result.get(content);
    assertEquals(profileSection, fut.get());
  }

  @Test
  public void testGetProvidedMapHandlesException() throws PersonProviderException,
      InterruptedException, ExecutionException {
    PersonProfileProviderAdapter pppa = new PersonProfileProviderAdapter();
    pppa.personProvider = this.personProvider;
    ArrayList<ProviderSettings> list = new ArrayList<ProviderSettings>();
    list.add(ps1);

    Content content = new Content(null, null);
    when(ps1.getNode()).thenReturn(content);
    String errorMessage = "Mocked error is a mock";
    when(this.personProvider.getProfileSection(org.mockito.Mockito.any(Content.class)))
        .thenThrow(new PersonProviderException(errorMessage));

    @SuppressWarnings("unchecked")
    Map<Content, Future<Map<String,Object>>> result = (Map<Content, Future<Map<String,Object>>>) pppa.getProvidedMap(list);
    Future<Map<String, Object>> fut = result.get(content);
    assertEquals(errorMessage, fut.get().get("error"));
  }
}
