import { test, expect, Page } from '@playwright/test'
import { uploadFile } from './TestUtils'

test('Test distribution diagram', async ({ page }) => {
  await page.goto('/')

  await uploadFile('result_small_cluster.zip', page)

  const options = getTestCombinations()
  selectOptions(page, options[0])
  const canvas = page.locator('canvas').first()
  let lastImage = await canvas.screenshot()
  for (const option of options.slice(1)) {
    await selectOptions(page, option)
    const newImage = await canvas.screenshot()
    expect(newImage).not.toEqual(lastImage)
    lastImage = newImage
  }
})

/**
 * Checks if the distribution diagram is correct for the given options
 * @param page Page currently tested on
 * @param options Options to be selected
 */
async function selectOptions(page: Page, options: string[]) {
  const distributionDiagramContainer = page.getByText('Distribution of Comparisons:Options:')
  for (const option of options) {
    await distributionDiagramContainer.getByText(option).first().click()
  }
  // This timeout is so that the screenshot is taken after the animation is finished
  await page.waitForTimeout(3000)
}

function getTestCombinations() {
  const options = [
    ['Average', 'Maximum'],
    ['Linear', 'Logarithmic']
  ]

  const combinations: string[][] = []

  const baseOptions = options.map((o) => o[0])
  for (let i = 0; i < options.length; i++) {
    for (let j = 0; j < options[i].length; j++) {
      const combination = Array.from(baseOptions)
      combination[i] = options[i][j]
      combinations.push(combination)
    }
  }
  return combinations
}
