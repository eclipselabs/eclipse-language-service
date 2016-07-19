package org.eclipse.languageserver;

import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.eclipse.json.provisonnal.com.eclipsesource.json.JsonArray;
import org.eclipse.json.provisonnal.com.eclipsesource.json.JsonObject;
import org.junit.Assert;
import org.junit.Test;

public class TestOmnisharpRequest {

	@Test
	public void test() throws Exception {
		HttpClient client = HttpClientBuilder.create().build();
		HttpPost post = new HttpPost("http://localhost:2000/autocomplete");
		String file = "/home/mistria/git/omnisharp-roslyn/src/OmniSharp.DotNetTest/Helpers/TestFeaturesDiscover.cs";
		String buffer = "public class TestFeaturesDiscover\n" +
"{\n" +
"   public static void Main()\n" +
"   {\n" +
"      System.Console.W\n" +
"   }\n" +
"}";
		JsonObject o = new JsonObject();
		o.set("Column", 21);
		o.set("Line", 5);
		o.set("Filename", file);
		o.set("Buffer", buffer);
		o.set("WantDocumentationForEveryCompletionResult", false);
		o.set("WordToComplete", "W");// TODO deduce from Buffer, line, column
		post.setEntity(new StringEntity(o.toString())); // depends on Buffer, line, column
		HttpResponse res = client.execute(post);
		Assert.assertEquals(HttpStatus.SC_OK, res.getStatusLine().getStatusCode());
		InputStream stream = res.getEntity().getContent();
		String stringResult = IOUtils.toString(stream);
		JsonArray result = JsonArray.readFrom(stringResult);
		stream.close();
//		Assert.assertEquals("Write", result.getString("word", ""));
		
	}

}
