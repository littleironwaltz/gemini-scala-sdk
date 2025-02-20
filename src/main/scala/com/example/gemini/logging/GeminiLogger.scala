package com.example.gemini.logging

import com.typesafe.scalalogging.LazyLogging
import com.example.gemini.GeminiError

/**
 * 共通ロギング機能を提供するトレイト
 * 
 * 特徴:
 * - コンテキストベースのログ出力
 * - APIキーの自動サニタイズ
 * - 一貫したログフォーマット
 * - エラー情報の構造化出力
 */
trait GeminiLogger extends LazyLogging {
  /**
   * 情報レベルのログを出力
   * 
   * @param context ログのコンテキスト（例: "AsyncGeminiAPI", "ConfigLoader"）
   * @param message ログメッセージ
   */
  protected def logInfo(context: String, message: String): Unit =
    logger.info(s"[$context] $message")

  /**
   * エラーレベルのログを出力（APIキーを自動的にマスク）
   * 
   * @param context ログのコンテキスト
   * @param error GeminiErrorインスタンス
   */
  protected def logError(context: String, error: GeminiError): Unit = {
    val sanitizedMessage = error.message.replaceAll("key=[^&]*", "key=REDACTED")
    logger.error(s"[$context] $sanitizedMessage")
  }

  /**
   * デバッグレベルのログを出力
   * 
   * @param context ログのコンテキスト
   * @param message ログメッセージ
   */
  protected def logDebug(context: String, message: String): Unit =
    logger.debug(s"[$context] $message")

  /**
   * リクエスト情報のログを出力
   * 
   * @param method HTTPメソッド
   * @param path リクエストパス
   */
  protected def logRequest(method: String, path: String): Unit =
    logDebug("Request", s"$method $path")
}
