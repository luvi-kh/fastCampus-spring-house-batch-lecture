package com.fastcampus.housebatch.job.apt;

import com.fastcampus.housebatch.adapter.ApartmentApiResource;
import com.fastcampus.housebatch.core.dto.AptDealDto;
import com.fastcampus.housebatch.core.repository.LawdRepository;
import com.fastcampus.housebatch.core.service.AptDealService;
import com.fastcampus.housebatch.job.validator.YearMonthParameterValidator;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.xml.StaxEventItemReader;
import org.springframework.batch.item.xml.builder.StaxEventItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

import java.time.YearMonth;

@Configuration
@AllArgsConstructor
@Slf4j
public class AptDealInsertJobConfig {
	
	private final JobBuilderFactory jobBuilderFactory;
	private final StepBuilderFactory stepBuilderFactory;
	
	private final ApartmentApiResource apartmentApiResource;
	
	@Bean
	public Job aptDealInsertJob(
		  Step guLawdCdStep,
		  Step aptDealInsertStep
	) {
		return jobBuilderFactory.get("aptDealInsertJob")
		  .incrementer(new RunIdIncrementer())
		  .validator(new YearMonthParameterValidator())
		  .start(guLawdCdStep)
		  .on("CONTINUABLE").to(aptDealInsertStep).next(guLawdCdStep)
		  .from(guLawdCdStep)
		  .on("*").end()
		  .end()
		  .build();
	}
	
	@JobScope
	@Bean
	public Step guLawdCdStep(Tasklet guLawdCdTasklet) {
		return stepBuilderFactory.get("guLawdCdStep")
		  .tasklet(guLawdCdTasklet)
		  .build();
	}
	
	@StepScope
	@Bean
	public Tasklet guLawdCdTasklet(LawdRepository lawdRepository) {
		return new GuLawdTasklet(lawdRepository);
	}
	
	@Bean
	@JobScope
	public Step aptDealInsertStep(StaxEventItemReader<AptDealDto> aptDealResourceReader,
	                              ItemWriter<AptDealDto> aptDealWriter) {
		return stepBuilderFactory.get("aptDealInsertStep")
		  .<AptDealDto, AptDealDto>chunk(10)
		  .reader(aptDealResourceReader)
		  .writer(aptDealWriter)
		  .build();
	}
	
	@Bean
	@StepScope
	public StaxEventItemReader<AptDealDto> aptDealResourceReader(
		  @Value("#{jobParameters['yearMonth']}") String yearMonth,
		  @Value("#{jobExecutionContext['guLawdCd']}") String guLawdCd,
		  Jaxb2Marshaller aptDealDtoMarshaller
	) {
		// 💡 네임스페이스 필터링 및 중첩 구조 우회를 위해 빌더를 쓰지 않고 리더 인스턴스를 직접 제어합니다.
		StaxEventItemReader<AptDealDto> reader = new StaxEventItemReader<>();
		reader.setName("aptDealResourceReader");
		reader.setResource(apartmentApiResource.getResource(guLawdCd, YearMonth.parse(yearMonth)));
		reader.setUnmarshaller(aptDealDtoMarshaller);

		// 💡 복잡한 상위 depth 구조와 상관없이 XML 안의 모든 단일 <item> 태그를 정밀 추적하도록 강제 지정합니다.
		reader.setFragmentRootElementName("item");

		return reader;
	}
	
	@Bean
	@StepScope
	public Jaxb2Marshaller aptDealDtoMarshaller() {
		Jaxb2Marshaller jaxb2Marshaller = new Jaxb2Marshaller();
		jaxb2Marshaller.setClassesToBeBound(AptDealDto.class);
		return jaxb2Marshaller;
	}
	
	@Bean
	@StepScope
	public ItemWriter<AptDealDto> aptDealWriter(AptDealService aptDealService) {
		return items -> {
			items.forEach(aptDealService::upsert);
			System.out.println("================= Writing Completed =================");
		};
	}
}
