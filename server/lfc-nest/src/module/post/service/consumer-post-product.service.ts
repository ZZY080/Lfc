import {
  BadRequestException,
  ConflictException,
  ForbiddenException,
  Injectable,
  NotFoundException,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { In, Repository } from 'typeorm';
import { PostProductEntity } from '@module/post/entity/post-product.entity';
import {
  PostProductBodyDto,
  PostProductDto,
} from '@module/post/dto/post-product.dto';
import {
  DeliveryMethod,
  PostProductCategory,
  PostProductStatus,
  ProductCondition,
} from '@shared/enum/product.enum';

@Injectable()
export class ConsumerPostProductService {
  constructor(
    @InjectRepository(PostProductEntity)
    private readonly productRepository: Repository<PostProductEntity>,
  ) {}

  async createForPost(postId: number, body: PostProductBodyDto) {
    const existing = await this.productRepository.findOne({
      where: { postId },
    });
    if (existing) {
      throw new ConflictException('该笔记已关联商品');
    }

    const product = this.productRepository.create({
      postId,
      price: this.normalizePrice(body.price),
      originalPrice: this.normalizeOptionalPrice(body.originalPrice),
      category: body.category ?? PostProductCategory.SECOND_HAND,
      condition: body.condition ?? ProductCondition.GOOD,
      deliveryMethod: body.deliveryMethod ?? DeliveryMethod.PICKUP,
      status: PostProductStatus.ON_SALE,
    });
    return this.productRepository.save(product);
  }

  async updateForPost(
    postId: number,
    authorId: number,
    body: PostProductBodyDto,
  ) {
    const product = await this.requireProductByPost(postId);
    if (product.post?.authorId !== authorId) {
      throw new ForbiddenException('无权修改商品信息');
    }
    if (product.status === PostProductStatus.SOLD) {
      throw new BadRequestException('已售出商品不可修改');
    }

    Object.assign(product, {
      price: this.normalizePrice(body.price),
      originalPrice: this.normalizeOptionalPrice(body.originalPrice),
      category: body.category ?? product.category,
      condition: body.condition ?? product.condition,
      deliveryMethod: body.deliveryMethod ?? product.deliveryMethod,
    });
    return this.productRepository.save(product);
  }

  async offShelf(postId: number, authorId: number) {
    const product = await this.requireProductByPost(postId);
    if (product.post?.authorId !== authorId) {
      throw new ForbiddenException('无权下架商品');
    }
    if (product.status === PostProductStatus.SOLD) {
      throw new BadRequestException('已售出商品不可下架');
    }
    product.status = PostProductStatus.OFF_SHELF;
    return this.productRepository.save(product);
  }

  async onShelf(postId: number, authorId: number) {
    const product = await this.requireProductByPost(postId);
    if (product.post?.authorId !== authorId) {
      throw new ForbiddenException('无权上架商品');
    }
    if (product.status === PostProductStatus.SOLD) {
      throw new BadRequestException('已售出商品不可重新上架');
    }
    product.status = PostProductStatus.ON_SALE;
    return this.productRepository.save(product);
  }

  async findByPostId(postId: number): Promise<PostProductEntity | null> {
    return this.productRepository.findOne({ where: { postId } });
  }

  async findMapByPostIds(
    postIds: number[],
  ): Promise<Map<number, PostProductEntity>> {
    if (postIds.length === 0) {
      return new Map();
    }
    const products = await this.productRepository.find({
      where: { postId: In(postIds) },
    });
    return new Map(products.map((item) => [item.postId, item]));
  }

  getProductPrice(product: PostProductEntity): number {
    return Number.parseFloat(String(product.price ?? 0));
  }

  async assertCanPurchase(userId: number, postId: number, authorId: number) {
    if (userId === authorId) {
      throw new BadRequestException('不能购买自己的商品');
    }

    const product = await this.findByPostId(postId);
    if (!product) {
      throw new NotFoundException('该笔记未挂载商品');
    }
    if (product.status !== PostProductStatus.ON_SALE) {
      throw new BadRequestException('商品当前不可购买');
    }
    return product;
  }

  async completePurchaseAfterPayment(userId: number, postId: number) {
    const product = await this.productRepository.findOne({
      where: { postId },
      relations: ['post'],
    });
    if (!product) {
      throw new NotFoundException('商品不存在');
    }
    if (product.post.authorId === userId) {
      throw new BadRequestException('不能购买自己的商品');
    }
    if (product.status === PostProductStatus.SOLD) {
      if (product.buyerId === userId) {
        return product;
      }
      throw new ConflictException('商品已售出');
    }
    if (product.status !== PostProductStatus.ON_SALE) {
      throw new BadRequestException('商品当前不可购买');
    }

    product.status = PostProductStatus.SOLD;
    product.buyerId = userId;
    product.soldAt = new Date();
    return this.productRepository.save(product);
  }

  async revertPurchaseAfterRefund(postId: number, buyerId: number) {
    const product = await this.productRepository.findOne({
      where: { postId },
    });
    if (!product) {
      throw new NotFoundException('商品不存在');
    }
    if (product.buyerId !== buyerId) {
      return product;
    }
    product.status = PostProductStatus.ON_SALE;
    product.buyerId = null;
    product.soldAt = null;
    return this.productRepository.save(product);
  }

  toDto(product: PostProductEntity): PostProductDto {
    return {
      id: product.id,
      postId: product.postId,
      price: product.price,
      originalPrice: product.originalPrice,
      category: product.category,
      condition: product.condition,
      deliveryMethod: product.deliveryMethod,
      status: product.status,
      buyerId: product.buyerId,
      soldAt: product.soldAt,
    };
  }

  private async requireProductByPost(postId: number) {
    const product = await this.productRepository.findOne({
      where: { postId },
      relations: ['post'],
    });
    if (!product) {
      throw new NotFoundException('商品不存在');
    }
    return product;
  }

  private normalizePrice(price: number): string {
    if (price <= 0) {
      throw new BadRequestException('商品价格必须大于0');
    }
    return price.toFixed(2);
  }

  private normalizeOptionalPrice(price?: number): string | null {
    if (price === undefined || price === null) {
      return null;
    }
    if (price <= 0) {
      throw new BadRequestException('原价必须大于0');
    }
    return price.toFixed(2);
  }
}
