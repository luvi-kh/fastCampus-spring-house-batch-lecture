package com.fastcampus.housebatch.core.dto;

import io.micrometer.core.instrument.util.StringUtils;
import lombok.Getter;
import lombok.ToString;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * 아파트 실거래가 api의 각각의 거래 정보를 담는 객체 (최신 영문 규격 반영)
 */
@ToString
@Getter
@XmlRootElement(name = "item")
public class AptDealDto {

	@XmlElement(name = "dealAmount") // 기존: 거래금액
	private String dealAmount;

	public Long getDealAmount(){
		return Long.parseLong(dealAmount.replaceAll(",", "").trim());
	}

	@XmlElement(name = "buildYear") // 기존: 건축년도
	private Integer builtYear;

	@XmlElement(name = "dealYear") // 기존: 년
	private Integer year;

	@XmlElement(name = "umdNm") // 기존: 법정동 -> 최신 API는 umdNm(읍면동명) 또는 aptDong으로 내려옵니다. 브라우저 화면에 보인 태그와 일치시킵니다.
	private String dong;

	@XmlElement(name = "aptNm") // 기존: 아파트
	private String aptName;

	@XmlElement(name = "dealMonth") // 기존: 월
	private Integer month;

	@XmlElement(name = "dealDay") // 기존: 일
	private Integer day;

	@XmlElement(name = "excluUseAr") // 기존: 전용면적 -> exclAvArea로 매핑
	private Double exclusiveArea;

	@XmlElement(name = "jibun") // 기존: 지번
	private String jibun;

	public String getJibun(){
		return Optional.ofNullable(jibun).orElse("");
	}

	@XmlElement(name = "sggCd") // 기존: 지역코드 -> sggCd(시군구코드)로 매핑
	private String regionalCode;

	@XmlElement(name = "floor") // 기존: 층
	private Integer floor;

	@XmlElement(name = "cstmrCcDttm") // 기존: 해제사유발생일 -> cstmrCcDttm으로 매핑되거나 공백일 수 있음
	private String dealCanceledDate;

	@XmlElement(name = "rgstToDd") // 기존: 해제여부 -> 최신 스펙 확인 필요 (일단 변수 유지를 위해 매핑 변경)
	private String dealCanceled;

	public LocalDate getDealCanceledDate(){
		if(StringUtils.isBlank(dealCanceledDate)){
			return null;
		}
		// 만약 데이터 포맷이 다를 경우(예: 20210730) 패턴 수정이 필요할 수 있습니다.
		return LocalDate.parse(dealCanceledDate.trim(), DateTimeFormatter.ofPattern("yy.MM.dd"));
	}

	public boolean isDealCanceled(){
		return "O".equals(dealCanceled) || StringUtils.isNotBlank(dealCanceledDate);
	}

	public LocalDate getDealDate(){
		return LocalDate.of(year, month, day);
	}
}
