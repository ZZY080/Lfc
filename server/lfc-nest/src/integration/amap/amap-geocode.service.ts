import { Injectable } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';

interface AmapRegeoResponse {
  status?: string;
  regeocode?: {
    formatted_address?: string;
  };
}

@Injectable()
export class AmapGeocodeService {
  constructor(private readonly configService: ConfigService) {}

  async reverseGeocode(
    longitude: number,
    latitude: number,
  ): Promise<string | null> {
    const key = this.configService.get<string>('AMAP_WEB_SERVICE_KEY')?.trim();
    if (!key) {
      return null;
    }

    try {
      const url = new URL('https://restapi.amap.com/v3/geocode/regeo');
      url.searchParams.set('key', key);
      url.searchParams.set('location', `${longitude},${latitude}`);
      url.searchParams.set('extensions', 'base');

      const response = await fetch(url);
      if (!response.ok) {
        return null;
      }

      const data = (await response.json()) as AmapRegeoResponse;
      if (data.status !== '1') {
        return null;
      }

      const formatted = data.regeocode?.formatted_address?.trim();
      return formatted || null;
    } catch {
      return null;
    }
  }
}
