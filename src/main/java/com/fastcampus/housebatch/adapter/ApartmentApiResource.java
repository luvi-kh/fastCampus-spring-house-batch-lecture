package com.fastcampus.housebatch.adapter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

@Component
@Slf4j
public class ApartmentApiResource {

	@Value("${external.apartment-api.path}")
	private String path;

	@Value("${external.apartment-api.service-key}")
	private String serviceKey;

	public Resource getResource(String lawdCd, YearMonth yearMonth) {
		// 1. 문자열 결합으로 인코딩 손상을 차단한 URL 생성
		String urlString = String.format("%s?serviceKey=%s&LAWD_CD=%s&DEAL_YMD=%s",
				path, serviceKey, lawdCd, yearMonth.format(DateTimeFormatter.ofPattern("yyyyMM")));

		try {
			// 2. 자바 순수 URL 커넥션 오픈
			URL url = new URL(urlString);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
			connection.setRequestProperty("Accept-Encoding", "gzip, deflate");
			connection.connect();

			// 3. GZIP 압축 여부 확인 및 스트림 처리
			String contentEncoding = connection.getContentEncoding();
			InputStream inputStream = connection.getInputStream();
			if ("gzip".equalsIgnoreCase(contentEncoding)) {
				inputStream = new GZIPInputStream(inputStream);
			}

			// 4. 원본 XML 데이터를 깨짐 없이 텍스트 문자열로 변환
			String xmlResponse;
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
				xmlResponse = reader.lines().collect(Collectors.joining("\n"));
			}

			if (xmlResponse == null || xmlResponse.isEmpty()) {
				throw new IllegalArgumentException("오픈 API로부터 받은 데이터가 비어있습니다.");
			}

			// 5. 완벽하게 획득한 XML 데이터를 안전한 바이트 리소스로 래핑하여 리더에게 전달
			return new ByteArrayResource(xmlResponse.getBytes(StandardCharsets.UTF_8)) {
				@Override
				public String getFilename() {
					return "response.xml";
				}
			};

		} catch (Exception e) {
			log.error("API 연동 중 네트워크 에러 발생: {}", e.getMessage());
			throw new IllegalArgumentException("Failed to fetch data from Open API", e);
		}
	}
}
