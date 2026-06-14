import { Global, Module } from '@nestjs/common';
import { AmapGeocodeService } from '@integration/amap/amap-geocode.service';

@Global()
@Module({
  providers: [AmapGeocodeService],
  exports: [AmapGeocodeService],
})
export class AmapModule {}
