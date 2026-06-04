import {
  DeliveryMethod,
  PostProductCategory,
  PostProductStatus,
  ProductCondition,
} from '@shared/enum/product.enum';

export interface PostProductBodyDto {
  price: number;
  originalPrice?: number;
  category?: PostProductCategory;
  condition?: ProductCondition;
  deliveryMethod?: DeliveryMethod;
}

export interface PostProductDto {
  id: number;
  postId: number;
  price: string;
  originalPrice: string | null;
  category: PostProductCategory;
  condition: ProductCondition;
  deliveryMethod: DeliveryMethod;
  status: PostProductStatus;
  buyerId: number | null;
  soldAt: Date | null;
}
