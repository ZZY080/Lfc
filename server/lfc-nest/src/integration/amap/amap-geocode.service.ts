import { Injectable, Logger } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';

interface AmapRegeoResponse {
  status?: string;
  regeocode?: AmapRegeocode;
}

interface AmapRegeocode {
  formatted_address?: unknown;
  addressComponent?: AmapAddressComponent;
  pois?: AmapRegeoPoi[];
  aois?: AmapRegeoAoi[];
}

interface AmapAddressComponent {
  province?: unknown;
  city?: unknown;
  district?: unknown;
  township?: unknown;
  streetNumber?: {
    street?: unknown;
    number?: unknown;
  };
}

interface AmapRegeoPoi {
  id?: unknown;
  name?: unknown;
  address?: unknown;
  location?: unknown;
  distance?: unknown;
  type?: unknown;
}

interface AmapRegeoAoi {
  id?: unknown;
  name?: unknown;
  location?: unknown;
  distance?: unknown;
}

export interface ReverseGeocodeResultDto {
  address: string | null;
  latitude?: number;
  longitude?: number;
}

interface AmapGeocodeGeoResponse {
  status?: string;
  geocodes?: AmapGeocodeItem[];
  count?: string;
}

interface AmapGeocodeItem {
  formatted_address?: unknown;
  location?: unknown;
  level?: unknown;
  province?: unknown;
  city?: unknown;
  district?: unknown;
}

interface AmapInputTipsResponse {
  status?: string;
  tips?: AmapInputTip[];
}

interface AmapInputTip {
  id?: unknown;
  name?: unknown;
  address?: unknown;
  location?: unknown;
  district?: unknown;
}

interface AmapPlaceTextResponse {
  status?: string;
  pois?: AmapPlacePoi[];
  count?: string;
  info?: string;
}

interface AmapPlaceChild {
  id?: unknown;
  name?: unknown;
  address?: unknown;
  location?: unknown;
  subtype?: unknown;
  typecode?: unknown;
  sname?: unknown;
}

interface AmapPlacePoi {
  id?: unknown;
  name?: unknown;
  address?: unknown;
  location?: unknown;
  entr_location?: unknown;
  pname?: unknown;
  cityname?: unknown;
  adname?: unknown;
  type?: unknown;
  typecode?: unknown;
  distance?: unknown;
  children?: AmapPlaceChild[];
}

export interface PlaceSuggestionDto {
  id: string;
  name: string;
  address: string;
  latitude: number;
  longitude: number;
  district: string;
}

export interface PlaceSearchResultDto {
  items: PlaceSuggestionDto[];
  hasMore: boolean;
}

interface ParsedPlaceCandidate {
  item: PlaceSuggestionDto;
  score: number;
}

@Injectable()
export class AmapGeocodeService {
  private readonly logger = new Logger(AmapGeocodeService.name);

  constructor(private readonly configService: ConfigService) {}

  private resolveWebServiceKey(): string | null {
    const webKey = this.configService.get<string>('AMAP_WEB_SERVICE_KEY')?.trim();
    if (webKey) {
      return webKey;
    }
    this.logger.warn(
      'AMAP_WEB_SERVICE_KEY is not configured; geocode APIs will return empty results',
    );
    return null;
  }

  async reverseGeocode(
    longitude: number,
    latitude: number,
  ): Promise<string | null> {
    const result = await this.reverseGeocodeDetail(longitude, latitude);
    return result.address;
  }

  async reverseGeocodeDetail(
    longitude: number,
    latitude: number,
  ): Promise<ReverseGeocodeResultDto> {
    const key = this.resolveWebServiceKey();
    if (!key) {
      return { address: null };
    }

    try {
      const url = new URL('https://restapi.amap.com/v3/geocode/regeo');
      url.searchParams.set('key', key);
      url.searchParams.set('location', `${longitude},${latitude}`);
      url.searchParams.set('extensions', 'all');
      url.searchParams.set('radius', '200');

      const response = await fetch(url);
      if (!response.ok) {
        return { address: null };
      }

      const data = (await response.json()) as AmapRegeoResponse;
      if (data.status !== '1' || !data.regeocode) {
        return { address: null };
      }

      return this.parseRegeocodeDetail(data.regeocode, latitude, longitude);
    } catch (error) {
      this.logger.warn(`reverseGeocodeDetail failed: ${String(error)}`);
      return { address: null };
    }
  }

  private parseRegeocodeDetail(
    regeocode: AmapRegeocode,
    fallbackLatitude: number,
    fallbackLongitude: number,
  ): ReverseGeocodeResultDto {
    const formattedAddress = this.normalizeAmapText(regeocode.formatted_address);
    const buildingPoi = this.pickNearestBuildingPoi(regeocode.pois ?? []);
    const nearestAoi = this.pickNearestAoi(regeocode.aois ?? []);

    let address = formattedAddress;
    let latitude = fallbackLatitude;
    let longitude = fallbackLongitude;

    if (buildingPoi) {
      const poiName = this.normalizeAmapText(buildingPoi.name);
      const poiAddress = this.normalizeAmapText(buildingPoi.address);
      const poiCoords = this.parseCoordinates(buildingPoi.location);
      if (poiCoords) {
        latitude = poiCoords.latitude;
        longitude = poiCoords.longitude;
      }
      if (poiName) {
        address = this.composePreciseAddress(
          formattedAddress,
          poiAddress,
          poiName,
          nearestAoi?.name ? this.normalizeAmapText(nearestAoi.name) : '',
        );
      }
    } else if (nearestAoi?.name) {
      const aoiName = this.normalizeAmapText(nearestAoi.name);
      if (aoiName && address && !address.includes(aoiName)) {
        address = `${address}${aoiName}`;
      }
    }

    if (!address) {
      address = this.composeAddressFromComponent(regeocode.addressComponent);
    }

    return {
      address: address || null,
      latitude,
      longitude,
    };
  }

  private pickNearestBuildingPoi(pois: AmapRegeoPoi[]): AmapRegeoPoi | null {
    const candidates = pois
      .map((poi) => {
        const name = this.normalizeAmapText(poi.name);
        const type = this.normalizeAmapText(poi.type);
        const distance = Number.parseFloat(this.normalizeAmapText(poi.distance));
        if (!name || !Number.isFinite(distance)) {
          return null;
        }
        const isBuilding =
          type.includes('楼栋号') ||
          type.includes('门牌信息') ||
          this.isBuildingLikePoi(name) ||
          this.containsBuildingNumber(name);
        if (!isBuilding || distance > 80) {
          return null;
        }
        return { poi, distance };
      })
      .filter((item): item is { poi: AmapRegeoPoi; distance: number } => item != null)
      .sort((a, b) => a.distance - b.distance);

    return candidates[0]?.poi ?? null;
  }

  private pickNearestAoi(aois: AmapRegeoAoi[]): AmapRegeoAoi | null {
    const candidates = aois
      .map((aoi) => {
        const distance = Number.parseFloat(this.normalizeAmapText(aoi.distance));
        if (!Number.isFinite(distance) || distance > 120) {
          return null;
        }
        return { aoi, distance };
      })
      .filter((item): item is { aoi: AmapRegeoAoi; distance: number } => item != null)
      .sort((a, b) => a.distance - b.distance);

    return candidates[0]?.aoi ?? null;
  }

  private composePreciseAddress(
    formattedAddress: string,
    poiAddress: string,
    poiName: string,
    aoiName: string,
  ): string {
    if (formattedAddress.includes(poiName)) {
      return formattedAddress;
    }
    if (poiAddress.includes(poiName)) {
      return poiAddress;
    }
    if (formattedAddress) {
      if (
        aoiName &&
        formattedAddress.includes(aoiName) &&
        this.containsBuildingNumber(poiName)
      ) {
        return `${formattedAddress.replace(aoiName, '')}${aoiName}${poiName}`.replace(
          /\s+/g,
          '',
        );
      }
      return `${formattedAddress}${poiName}`;
    }
    return poiAddress || poiName;
  }

  private composeAddressFromComponent(
    component?: AmapAddressComponent,
  ): string {
    if (!component) {
      return '';
    }
    const parts = [
      this.normalizeAmapText(component.province),
      this.normalizeAmapText(component.city),
      this.normalizeAmapText(component.district),
      this.normalizeAmapText(component.township),
      this.normalizeAmapText(component.streetNumber?.street),
      this.normalizeAmapText(component.streetNumber?.number),
    ].filter(Boolean);
    return parts.join('');
  }

  async searchPlaces(
    keyword: string,
    page = 1,
    limit = 20,
    options?: {
      city?: string;
      latitude?: number;
      longitude?: number;
    },
  ): Promise<PlaceSearchResultDto> {
    const trimmed = keyword.trim();
    if (!trimmed) {
      return { items: [], hasMore: false };
    }

    const key = this.resolveWebServiceKey();
    if (!key) {
      return { items: [], hasMore: false };
    }

    const normalizedPage = Math.max(1, page);
    const normalizedLimit = Math.min(Math.max(limit, 1), 25);
    const city = this.normalizeSearchCity(options?.city);
    const latitude = options?.latitude;
    const longitude = options?.longitude;
    const hasBias =
      Number.isFinite(latitude) &&
      Number.isFinite(longitude) &&
      latitude != null &&
      longitude != null;

    try {
      const textResult = await this.fetchPlaceText(
        key,
        trimmed,
        normalizedPage,
        normalizedLimit,
        city,
      );

      let aroundResult: AmapPlaceTextResponse | null = null;
      let geocodeCandidates: ParsedPlaceCandidate[] = [];
      let inputTipCandidates: ParsedPlaceCandidate[] = [];
      if (normalizedPage === 1) {
        const auxiliaryCity = this.resolveAuxiliarySearchCity(trimmed, city);
        geocodeCandidates = await this.fetchGeocodeCandidates(
          key,
          trimmed,
          auxiliaryCity,
        );
        inputTipCandidates = await this.fetchInputTipCandidates(
          key,
          trimmed,
          auxiliaryCity,
        );
        if (hasBias) {
          aroundResult = await this.fetchPlaceAround(
            key,
            trimmed,
            longitude!,
            latitude!,
            normalizedLimit,
          );
        }
      }

      const candidates: ParsedPlaceCandidate[] = [];
      candidates.push(...geocodeCandidates);
      candidates.push(...inputTipCandidates);
      for (const poi of textResult.pois ?? []) {
        candidates.push(...this.expandPoiCandidates(poi, trimmed, false));
      }
      for (const poi of aroundResult?.pois ?? []) {
        candidates.push(...this.expandPoiCandidates(poi, trimmed, true));
      }

      const items = this.rankAndDedupeCandidates(candidates, normalizedLimit);
      const textCount = Number.parseInt(textResult.count ?? '0', 10);
      const textHasMore =
        textCount > 0
          ? normalizedPage * normalizedLimit < textCount
          : (textResult.pois?.length ?? 0) >= normalizedLimit;

      return {
        items,
        hasMore: textHasMore,
      };
    } catch (error) {
      this.logger.error(`searchPlaces failed for keyword="${trimmed}": ${String(error)}`);
      return { items: [], hasMore: false };
    }
  }

  private normalizeSearchCity(city?: string): string | undefined {
    const normalized = this.normalizeAmapText(city);
    if (!normalized || normalized === '同城') {
      return undefined;
    }
    return normalized;
  }

  private resolveAuxiliarySearchCity(
    keyword: string,
    biasCity?: string,
  ): string | undefined {
    const fromKeyword = this.extractCityFromKeyword(keyword);
    if (fromKeyword) {
      return fromKeyword;
    }
    if (this.looksLikeStructuredAddress(keyword)) {
      return undefined;
    }
    return biasCity;
  }

  private extractCityFromKeyword(text: string): string | undefined {
    const municipalityMatch = text.match(
      /(北京|上海|天津|重庆)(?:市|城区|市区)?/,
    );
    if (municipalityMatch) {
      return municipalityMatch[1];
    }

    const cityMatch = text.match(/([\u4e00-\u9fa5]{2,10})市/);
    if (cityMatch) {
      return cityMatch[1].replace(/市$/, '');
    }

    return undefined;
  }

  private looksLikeStructuredAddress(text: string): boolean {
    return /省|自治区|特别行政区|市|区|县|镇|乡|村|路|街|巷|弄|号|栋|幢|座|楼/.test(
      text,
    );
  }

  private async fetchGeocodeCandidates(
    key: string,
    keyword: string,
    city?: string,
  ): Promise<ParsedPlaceCandidate[]> {
    const url = new URL('https://restapi.amap.com/v3/geocode/geo');
    url.searchParams.set('key', key);
    url.searchParams.set('address', keyword);
    if (city) {
      url.searchParams.set('city', city);
    }

    const response = await fetch(url);
    if (!response.ok) {
      return [];
    }

    const data = (await response.json()) as AmapGeocodeGeoResponse;
    if (data.status !== '1' || !data.geocodes?.length) {
      return [];
    }

    const candidates: ParsedPlaceCandidate[] = [];
    for (const geocode of data.geocodes) {
      const candidate = this.parseGeocodeItem(geocode, keyword);
      if (candidate) {
        candidates.push(candidate);
      }
    }
    return candidates;
  }

  private async fetchInputTipCandidates(
    key: string,
    keyword: string,
    city?: string,
  ): Promise<ParsedPlaceCandidate[]> {
    const url = new URL('https://restapi.amap.com/v3/assistant/inputtips');
    url.searchParams.set('key', key);
    url.searchParams.set('keywords', keyword);
    url.searchParams.set('datatype', 'all');
    if (city) {
      url.searchParams.set('city', city);
    }

    const response = await fetch(url);
    if (!response.ok) {
      return [];
    }

    const data = (await response.json()) as AmapInputTipsResponse;
    if (data.status !== '1' || !data.tips?.length) {
      return [];
    }

    const candidates: ParsedPlaceCandidate[] = [];
    for (const tip of data.tips) {
      const candidate = this.parseInputTip(tip, keyword);
      if (candidate) {
        candidates.push(candidate);
      }
    }
    return candidates;
  }

  private parseGeocodeItem(
    geocode: AmapGeocodeItem,
    keyword: string,
  ): ParsedPlaceCandidate | null {
    const formattedAddress = this.normalizeAmapText(geocode.formatted_address);
    const coords = this.parseCoordinates(geocode.location);
    if (!formattedAddress || !coords) {
      return null;
    }

    const district = [
      this.normalizeAmapText(geocode.province),
      this.normalizeAmapText(geocode.city),
      this.normalizeAmapText(geocode.district),
    ]
      .filter(Boolean)
      .join('');

    const level = this.normalizeAmapText(geocode.level);
    const displayName = this.extractBuildingLabel(formattedAddress, keyword);

    const item: PlaceSuggestionDto = {
      id: `geo-${formattedAddress}-${coords.longitude},${coords.latitude}`,
      name: displayName,
      address: formattedAddress,
      latitude: coords.latitude,
      longitude: coords.longitude,
      district,
    };

    let score = 40;
    if (this.containsBuildingNumber(keyword)) {
      score += 35;
    }
    if (formattedAddress.includes(keyword.trim())) {
      score += 25;
    }
    if (level === '兴趣点' || level === '住宅区' || level === '楼栋') {
      score += 20;
    }
    if (this.isBuildingLikePoi(displayName)) {
      score += 15;
    }

    return { item, score };
  }

  private parseInputTip(
    tip: AmapInputTip,
    keyword: string,
  ): ParsedPlaceCandidate | null {
    const name = this.normalizeAmapText(tip.name);
    const coords = this.parseCoordinates(tip.location);
    if (!name || !coords) {
      return null;
    }

    const tipAddress = this.normalizeAmapText(tip.address);
    const district = this.normalizeAmapText(tip.district);
    const fullAddress = tipAddress || name;
    const displayName = this.extractBuildingLabel(name, keyword);

    const item: PlaceSuggestionDto = {
      id:
        this.normalizeAmapText(tip.id) ||
        `tip-${displayName}-${coords.longitude},${coords.latitude}`,
      name: displayName,
      address: this.composeFullAddress(district, fullAddress, displayName),
      latitude: coords.latitude,
      longitude: coords.longitude,
      district,
    };

    let score = 28;
    if (this.containsBuildingNumber(name) || this.containsBuildingNumber(tipAddress)) {
      score += 32;
    }
    if (name.includes(keyword.trim()) || tipAddress.includes(keyword.trim())) {
      score += 22;
    }
    if (this.isBuildingLikePoi(displayName)) {
      score += 18;
    }

    return { item, score };
  }

  private containsBuildingNumber(text: string): boolean {
    return /(\d+|[一二三四五六七八九十百千]+)\s*(号楼|栋|幢|座|单元)/.test(
      text,
    );
  }

  private extractBuildingLabel(address: string, keyword: string): string {
    const trimmedKeyword = keyword.trim();
    const buildingFromKeyword = trimmedKeyword.match(
      /([\u4e00-\u9fa5A-Za-z0-9·]+?(?:\d+|[一二三四五六七八九十百千]+)\s*(?:号楼|栋|幢|座))/,
    );
    if (buildingFromKeyword && address.includes(buildingFromKeyword[1])) {
      return buildingFromKeyword[1].replace(/\s+/g, '');
    }

    const buildingFromAddress = address.match(
      /([\u4e00-\u9fa5A-Za-z0-9·]+?(?:\d+|[一二三四五六七八九十百千]+)\s*(?:号楼|栋|幢|座))/,
    );
    if (buildingFromAddress) {
      return buildingFromAddress[1].replace(/\s+/g, '');
    }

    return address;
  }

  private async fetchPlaceText(
    key: string,
    keyword: string,
    page: number,
    limit: number,
    city?: string,
  ): Promise<AmapPlaceTextResponse> {
    const url = new URL('https://restapi.amap.com/v3/place/text');
    url.searchParams.set('key', key);
    url.searchParams.set('keywords', keyword);
    url.searchParams.set('offset', String(limit));
    url.searchParams.set('page', String(page));
    url.searchParams.set('extensions', 'all');
    url.searchParams.set('children', '1');
    if (city) {
      url.searchParams.set('city', city);
      url.searchParams.set('citylimit', 'false');
    }

    const response = await fetch(url);
    if (!response.ok) {
      return { status: '0', pois: [] };
    }

    const data = (await response.json()) as AmapPlaceTextResponse;
    if (data.status !== '1') {
      return { status: '0', pois: [], count: '0' };
    }
    return data;
  }

  private async fetchPlaceAround(
    key: string,
    keyword: string,
    longitude: number,
    latitude: number,
    limit: number,
  ): Promise<AmapPlaceTextResponse | null> {
    const url = new URL('https://restapi.amap.com/v3/place/around');
    url.searchParams.set('key', key);
    url.searchParams.set('keywords', keyword);
    url.searchParams.set('location', `${longitude},${latitude}`);
    url.searchParams.set('radius', '3000');
    url.searchParams.set('offset', String(limit));
    url.searchParams.set('page', '1');
    url.searchParams.set('extensions', 'all');
    url.searchParams.set('children', '1');
    url.searchParams.set('sortrule', 'distance');

    const response = await fetch(url);
    if (!response.ok) {
      return null;
    }

    const data = (await response.json()) as AmapPlaceTextResponse;
    if (data.status !== '1') {
      return null;
    }
    return data;
  }

  private expandPoiCandidates(
    poi: AmapPlacePoi,
    keyword: string,
    fromAround: boolean,
  ): ParsedPlaceCandidate[] {
    const children = poi.children ?? [];
    const buildingChildren = children.filter((child) =>
      this.isBuildingLikePoi(
        this.normalizeAmapText(child.name, child.sname),
        this.normalizeAmapText(child.subtype),
      ),
    );

    if (buildingChildren.length > 0) {
      const childCandidates = buildingChildren
        .map((child) => this.toCandidateFromChild(poi, child, keyword, fromAround))
        .filter((candidate): candidate is ParsedPlaceCandidate => candidate != null);
      if (childCandidates.length > 0) {
        return childCandidates;
      }
    }

    const parent = this.toCandidateFromPoi(poi, keyword, fromAround);
    return parent ? [parent] : [];
  }

  private toCandidateFromPoi(
    poi: AmapPlacePoi,
    keyword: string,
    fromAround: boolean,
  ): ParsedPlaceCandidate | null {
    const parsed = this.parsePlacePoi(poi, null);
    if (!parsed) {
      return null;
    }

    const distance = Number.parseFloat(this.normalizeAmapText(poi.distance));
    return {
      item: parsed,
      score: this.scorePlace(parsed, keyword, {
        fromAround,
        distanceMeters: Number.isFinite(distance) ? distance : null,
        hasEntrance: this.isCoordinateText(this.normalizeAmapText(poi.entr_location)),
      }),
    };
  }

  private toCandidateFromChild(
    parent: AmapPlacePoi,
    child: AmapPlaceChild,
    keyword: string,
    fromAround: boolean,
  ): ParsedPlaceCandidate | null {
    const parentName = this.normalizeAmapText(parent.name);
    const childName = this.normalizeAmapText(child.name, child.sname);
    const displayName =
      childName && parentName && !childName.includes(parentName)
        ? `${parentName}${childName}`
        : childName || parentName;

    if (!displayName) {
      return null;
    }

    const parentAddress = this.formatStreetAddress(parent);
    const childAddress = this.normalizeAmapText(child.address);
    const streetAddress = childAddress || parentAddress;
    const district = this.formatDistrict(parent);
    const fullAddress = this.composeFullAddress(district, streetAddress, displayName);

    const coords = this.parseCoordinates(
      child.location,
      parent.entr_location,
      parent.location,
    );
    if (!coords) {
      return null;
    }

    const item: PlaceSuggestionDto = {
      id:
        this.normalizeAmapText(child.id) ||
        `${displayName}-${coords.longitude},${coords.latitude}`,
      name: displayName,
      address: fullAddress,
      latitude: coords.latitude,
      longitude: coords.longitude,
      district,
    };

    const distance = Number.parseFloat(this.normalizeAmapText(parent.distance));
    return {
      item,
      score: this.scorePlace(item, keyword, {
        fromAround,
        distanceMeters: Number.isFinite(distance) ? distance : null,
        hasEntrance: true,
        isChildPoi: true,
      }),
    };
  }

  private parsePlacePoi(
    poi: AmapPlacePoi,
    parentName: string | null,
  ): PlaceSuggestionDto | null {
    const rawName = this.normalizeAmapText(poi.name);
    if (!rawName) {
      return null;
    }

    const name =
      parentName && !rawName.includes(parentName)
        ? `${parentName}${rawName}`
        : rawName;

    const coords = this.parseCoordinates(
      poi.entr_location,
      poi.location,
    );
    if (!coords) {
      return null;
    }

    const district = this.formatDistrict(poi);
    const streetAddress = this.formatStreetAddress(poi);
    const fullAddress = this.composeFullAddress(district, streetAddress, name);

    return {
      id:
        this.normalizeAmapText(poi.id) ||
        `${name}-${coords.longitude},${coords.latitude}`,
      name,
      address: fullAddress,
      latitude: coords.latitude,
      longitude: coords.longitude,
      district,
    };
  }

  private parseCoordinates(...values: unknown[]): {
    latitude: number;
    longitude: number;
  } | null {
    for (const value of values) {
      const parsed = this.parseLocation(this.normalizeAmapText(value));
      if (parsed) {
        return parsed;
      }
    }
    return null;
  }

  private parseLocation(location: string): {
    latitude: number;
    longitude: number;
  } | null {
    if (!this.isCoordinateText(location)) {
      return null;
    }

    const [lngRaw, latRaw] = location.split(',');
    const longitude = Number.parseFloat(lngRaw);
    const latitude = Number.parseFloat(latRaw);
    if (!Number.isFinite(longitude) || !Number.isFinite(latitude)) {
      return null;
    }

    return { latitude, longitude };
  }

  private isCoordinateText(value: string): boolean {
    return /^-?\d+(\.\d+)?,-?\d+(\.\d+)?$/.test(value);
  }

  private normalizeAmapText(...values: unknown[]): string {
    for (const value of values) {
      if (typeof value === 'string') {
        const trimmed = value.trim();
        if (trimmed && trimmed !== '[]') {
          return trimmed;
        }
      }
      if (Array.isArray(value)) {
        const joined = value
          .map((part) => (typeof part === 'string' ? part.trim() : ''))
          .filter((part) => part && part !== '[]')
          .join('');
        if (joined) {
          return joined;
        }
      }
    }
    return '';
  }

  private formatDistrict(poi: AmapPlacePoi): string {
    return [poi.pname, poi.cityname, poi.adname]
      .map((part) => this.normalizeAmapText(part))
      .filter(Boolean)
      .join('');
  }

  private formatStreetAddress(poi: AmapPlacePoi): string {
    return this.normalizeAmapText(poi.address);
  }

  private composeFullAddress(
    district: string,
    streetAddress: string,
    name: string,
  ): string {
    const parts: string[] = [];
    if (district && !streetAddress.includes(district)) {
      parts.push(district);
    }
    if (streetAddress) {
      parts.push(streetAddress);
    }
    if (name && !streetAddress.includes(name) && !district.includes(name)) {
      parts.push(name);
    }
    return parts.join('') || name;
  }

  private isBuildingLikePoi(name?: string, subtype?: string): boolean {
    const label = `${name ?? ''} ${subtype ?? ''}`.trim();
    if (!label) {
      return false;
    }
    if (/停车|停车场|车库|出入口|大门|门$/.test(label)) {
      return false;
    }
    return /号楼|栋|座|单元|宿舍|教学楼|实验楼|综合楼|办公楼|寝室|公寓|楼|馆|院|舍|坊|大厦|中心/.test(
      label,
    );
  }

  private scorePlace(
    item: PlaceSuggestionDto,
    keyword: string,
    hints: {
      fromAround: boolean;
      distanceMeters: number | null;
      hasEntrance: boolean;
      isChildPoi?: boolean;
    },
  ): number {
    const normalizedKeyword = keyword.trim();
    const name = item.name;
    const address = item.address;
    let score = 0;

    if (normalizedKeyword && name.includes(normalizedKeyword)) {
      score += 30;
    }
    if (normalizedKeyword && address.includes(normalizedKeyword)) {
      score += 12;
    }
    if (this.isBuildingLikePoi(item.name)) {
      score += 18;
    }
    if (hints.isChildPoi) {
      score += 14;
    }
    if (hints.hasEntrance) {
      score += 6;
    }
    if (hints.fromAround) {
      score += 10;
      if (hints.distanceMeters != null) {
        score += Math.max(0, 20 - hints.distanceMeters / 150);
      }
    }

    return score;
  }

  private rankAndDedupeCandidates(
    candidates: ParsedPlaceCandidate[],
    limit: number,
  ): PlaceSuggestionDto[] {
    const bestById = new Map<string, ParsedPlaceCandidate>();

    for (const candidate of candidates) {
      const existing = bestById.get(candidate.item.id);
      if (!existing || candidate.score > existing.score) {
        bestById.set(candidate.item.id, candidate);
      }
    }

    const dedupedByLocation = new Map<string, ParsedPlaceCandidate>();
    for (const candidate of bestById.values()) {
      const locationKey = `${candidate.item.longitude.toFixed(5)},${candidate.item.latitude.toFixed(5)}`;
      const existing = dedupedByLocation.get(locationKey);
      if (!existing || candidate.score > existing.score) {
        dedupedByLocation.set(locationKey, candidate);
      }
    }

    return [...dedupedByLocation.values()]
      .sort((a, b) => b.score - a.score)
      .slice(0, limit)
      .map((candidate) => candidate.item);
  }
}
