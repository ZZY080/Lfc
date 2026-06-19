import {
  DeliveryMethod,
  PostProductCategory,
  PostProductStatus,
  ProductCondition,
} from '@shared/enum/product.enum';

export interface AdminPostRelationUserDto {
  id: number;
  email: string;
  studentId: string;
  realName: string;
  nickname: string | null;
}

export interface AdminPostProductDto {
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
  createdAt: Date;
  updatedAt: Date;
}

export interface AdminPostLikeItemDto {
  id: number;
  postId: number;
  userId: number;
  user: AdminPostRelationUserDto;
  createdAt: Date;
}

export interface AdminPostFavoriteItemDto {
  id: number;
  postId: number;
  userId: number;
  user: AdminPostRelationUserDto;
  createdAt: Date;
}

export interface AdminUpdatePostProductBodyDto {
  price?: number;
  originalPrice?: number | null;
  category?: PostProductCategory;
  condition?: ProductCondition;
  deliveryMethod?: DeliveryMethod;
  status?: PostProductStatus;
}
