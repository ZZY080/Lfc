import { Controller, Get, Query } from '@nestjs/common';
import { AmapGeocodeService } from '@integration/amap/amap-geocode.service';

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

    const address = await this.amapGeocodeService.reverseGeocode(lng, lat);
    return { address };
  }
}
