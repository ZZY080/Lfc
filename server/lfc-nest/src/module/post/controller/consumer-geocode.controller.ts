import { Controller, Get, Query } from '@nestjs/common';
import { AmapGeocodeService } from '@integration/amap/amap-geocode.service';
import { Type } from 'class-transformer';
import {
  IsInt,
  IsNumber,
  IsOptional,
  IsString,
  Max,
  MaxLength,
  Min,
} from 'class-validator';

class PlaceSearchQuerySchema {
  @IsString()
  @MaxLength(64)
  keyword: string;

  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(1)
  page?: number;

  @IsOptional()
  @Type(() => Number)
  @IsInt()
  @Min(1)
  @Max(25)
  limit?: number;

  @IsOptional()
  @IsString()
  @MaxLength(32)
  city?: string;

  @IsOptional()
  @Type(() => Number)
  @IsNumber()
  @Min(-90)
  @Max(90)
  latitude?: number;

  @IsOptional()
  @Type(() => Number)
  @IsNumber()
  @Min(-180)
  @Max(180)
  longitude?: number;
}

@Controller('consumer/geocode')
export class ConsumerGeocodeController {
  constructor(private readonly amapGeocodeService: AmapGeocodeService) {}

  @Get('reverse')
  async reverse(
    @Query('latitude') latitude: string,
    @Query('longitude') longitude: string,
  ) {
    const lat = Number.parseFloat(latitude);
    const lng = Number.parseFloat(longitude);
    if (!Number.isFinite(lat) || !Number.isFinite(lng)) {
      return { address: null };
    }

    return this.amapGeocodeService.reverseGeocodeDetail(lng, lat);
  }

  @Get('search')
  search(@Query() query: PlaceSearchQuerySchema) {
    return this.amapGeocodeService.searchPlaces(
      query.keyword,
      query.page,
      query.limit,
      {
        city: query.city,
        latitude: query.latitude,
        longitude: query.longitude,
      },
    );
  }
}
