package com.andreyferraz.gestao.config;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.jdbc.core.convert.JdbcCustomConversions;
import org.springframework.data.jdbc.core.dialect.JdbcDialect;
import org.springframework.data.relational.core.dialect.AnsiDialect;
import org.springframework.data.relational.core.dialect.LimitClause;
import org.springframework.data.relational.core.dialect.LockClause;
import org.springframework.data.relational.core.sql.render.SelectRenderContext;
import org.springframework.data.convert.ReadingConverter;

@Configuration
public class DbConfig {

	@Bean
	JdbcCustomConversions jdbcCustomConversions() {
		return new JdbcCustomConversions(List.of(new NumberToLocalDateConverter()));
	}

	@Bean
	JdbcDialect jdbcDialect() {
		return new JdbcDialect() {
			@Override
			public LimitClause limit() {
				return AnsiDialect.INSTANCE.limit();
			}

			@Override
			public LockClause lock() {
				return AnsiDialect.INSTANCE.lock();
			}

			@Override
			public SelectRenderContext getSelectContext() {
				return AnsiDialect.INSTANCE.getSelectContext();
			}
		};
	}

	@ReadingConverter
	static class NumberToLocalDateConverter implements Converter<Number, LocalDate> {

		@Override
		public LocalDate convert(Number source) {
			if (source == null) {
				return null;
			}

			long epochValue = source.longValue();
			if (Math.abs(epochValue) > 9_999_999_999L) {
				return Instant.ofEpochMilli(epochValue).atZone(ZoneOffset.UTC).toLocalDate();
			}

			return Instant.ofEpochSecond(epochValue).atZone(ZoneOffset.UTC).toLocalDate();
		}
	}

}
